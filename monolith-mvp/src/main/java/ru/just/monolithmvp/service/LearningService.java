package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.learning.PendingSubmissionDto;
import ru.just.monolithmvp.dto.learning.PracticeSubmissionRequest;
import ru.just.monolithmvp.dto.learning.ReviewOpenSubmissionRequest;
import ru.just.monolithmvp.dto.learning.SubmissionResultDto;
import ru.just.monolithmvp.dto.lesson.LearnerLessonDto;
import ru.just.monolithmvp.dto.lesson.LearnerPracticeQuestionDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.observability.BusinessEventLogger;
import ru.just.monolithmvp.repository.*;
import ru.just.monolithmvp.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningService {
    private final CourseService courseService;
    private final LessonRepository lessonRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AppUserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final BusinessEventLogger businessEventLogger;
    private final ProgramService programService;

    @Transactional
    public LearnerLessonDto getLessonForLearner(Long lessonId, Long userId) {
        final Lesson lesson = courseService.getLessonEntity(lessonId);
        validateStudentEnrolled(userId, lesson.getCourse().getId());
        boolean lessonAlreadyPassed = isLessonPassedByStudent(userId, lessonId);
        if (!lessonAlreadyPassed) {
            assertLessonAccessAllowed(userId, lesson);
            assertStopLessonAccessAllowed(userId, lesson);
            courseService.assertCourseDeadlineNotExceededForStudent(userId, lesson.getCourse().getId());
        }

        LearnerLessonDto.LearnerLessonDtoBuilder learnerLessonDtoBuilder = LearnerLessonDto.builder()
                .id(lesson.getId())
                .position(lesson.getPosition())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .lessonType(lesson.getLessonType());
        if (LessonType.LessonSubType.PRACTICE.equals(lesson.getLessonType().getSubType())) {
            final List<PracticeQuestion> practiceQuestions = selectPracticeQuestionsForAttempt((PracticeLesson) lesson);
            final List<LearnerPracticeQuestionDto> questions = practiceQuestions.stream().map(question -> new LearnerPracticeQuestionDto(
                    question.getQuestionIndex(),
                    question.getQuestionType(),
                    question.getQuestionText(),
                    courseService.splitRaw(question.getOptionsRaw()),
                    question.getFullPoints(),
                    question.getPartialPoints()
            )).toList();
            learnerLessonDtoBuilder.questions(questions);
        } else {
            TheoryLesson theoryLesson = (TheoryLesson) lesson;
            learnerLessonDtoBuilder
                    .theoryContentType(theoryLesson.getContentType())
                    .theoryContent(theoryLesson.getContent());
        }
        return learnerLessonDtoBuilder.build();
    }

    @Transactional(readOnly = true)
    public LearnerLessonDto getNextLessonForLearner(Long courseId, Long userId) {
        Long nextLessonId = courseService.findNextLessonIdForLearner(userId, courseId);
        if (nextLessonId == null) {
            throw new BadRequestException("No next lesson available");
        }
        return getLessonForLearner(nextLessonId, userId);
    }

    @Transactional
    public SubmissionResultDto completeTheoryLesson(Long lessonId, Long userId) {
        Lesson lesson = courseService.getLessonEntity(lessonId);
        validateStudentEnrolled(userId, lesson.getCourse().getId());
        assertLessonAccessAllowed(userId, lesson);
        assertStopLessonAccessAllowed(userId, lesson);
        courseService.assertCourseDeadlineNotExceededForStudent(userId, lesson.getCourse().getId());

        if (!isTheoryLesson(lesson)) {
            throw new BadRequestException("Lesson is not THEORY");
        }

        LessonSubmission submission = completeTheoryLessonInternal(userId, lesson);
        return new SubmissionResultDto(
                submission.getId(),
                submission.getStatus(),
                submission.getCompleted(),
                "Theory lesson completed"
        );
    }

    private List<PracticeQuestion> selectPracticeQuestionsForAttempt(PracticeLesson practiceLesson) {
        List<PracticeQuestion> selectedQuestions = new ArrayList<>(courseService.getPracticeQuestionForLesson(practiceLesson.getId()));

        Integer randomQuestionCount = practiceLesson.getRandomQuestionCount();
        if (randomQuestionCount != null && randomQuestionCount > 0 && randomQuestionCount < selectedQuestions.size()) {
            Collections.shuffle(selectedQuestions);
            selectedQuestions = new ArrayList<>(selectedQuestions.subList(0, randomQuestionCount));
        }

        if (Boolean.TRUE.equals(practiceLesson.getShuffleOnEveryAttempt())) {
            Collections.shuffle(selectedQuestions);
        } else {
            selectedQuestions.sort(Comparator.comparing(PracticeQuestion::getQuestionIndex));
        }

        return selectedQuestions;
    }

    @Transactional
    public SubmissionResultDto submitPractice(Long lessonId, PracticeSubmissionRequest request) {
        Long studentId = securityUtils.currentUserId();
        Lesson lesson = courseService.getLessonEntity(lessonId);
        validateStudentEnrolled(studentId, lesson.getCourse().getId());
        assertLessonAccessAllowed(studentId, lesson);
        assertStopLessonAccessAllowed(studentId, lesson);
        courseService.assertCourseDeadlineNotExceededForStudent(studentId, lesson.getCourse().getId());

        if (lesson instanceof PracticeLesson practiceLesson) {
            validatePracticeAttemptLimit(studentId, lessonId, practiceLesson.getAttemptLimit());
            validatePracticeTimeLimit(studentId, lessonId, practiceLesson.getTimeLimitMinutes());
        }

        if (!(lesson instanceof PracticeLesson practiceLesson)
                || (lesson.getLessonType() != LessonType.PRACTICE_TEST
                && lesson.getLessonType() != LessonType.PRACTICE_OPEN_ANSWER)) {
            throw new BadRequestException("Lesson is not PRACTICE");
        }

        LessonSubmission submission;
        markEnrollmentStarted(studentId, lesson.getCourse().getId());

        Map<Integer, List<String>> answersByQuestion = validateAndNormalizeAnswersByQuestion(practiceLesson, request);

        if (lesson.getLessonType() == LessonType.PRACTICE_OPEN_ANSWER) {
            if (practiceLesson.getQuestions().stream().anyMatch(q -> q.getQuestionType() != QuestionType.OPEN_ANSWER)) {
                throw new BadRequestException("PRACTICE_OPEN_ANSWER lesson must contain only OPEN_ANSWER questions");
            }

            Optional<LessonSubmission> reworkSubmission = submissionRepository
                    .findFirstByStudentIdAndLessonIdAndStatusOrderBySubmittedAtDesc(studentId, lessonId, SubmissionStatus.REWORK);

            SubmissionStatus previousStatus = null;
            if (reworkSubmission.isPresent()) {
                submission = reworkSubmission.get();
                previousStatus = submission.getStatus();
            } else {
                submission = new LessonSubmission();
                submission.setLesson(lesson);
                submission.setStudent(getStudent(studentId));
            }

            submission.setAnswerRaw(serializeAnswersByQuestion(answersByQuestion));
            submission.setStatus(SubmissionStatus.PENDING_REVIEW);
            submission.setCompleted(false);
            submission.setPointsAwarded(0);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setReviewedByAdminId(null);
            submission.setReviewedAt(null);
            submission.setReviewComment(null);

            submission = submissionRepository.save(submission);
            return new SubmissionResultDto(
                    submission.getId(),
                    submission.getStatus(),
                    false,
                    "Answer submitted and waiting for admin review"
            );
        }

        submission = new LessonSubmission();
        submission.setLesson(lesson);
        submission.setStudent(getStudent(studentId));
        submission.setSubmittedAt(LocalDateTime.now());

        if (practiceLesson.getQuestions().stream().anyMatch(q -> q.getQuestionType() == QuestionType.OPEN_ANSWER)) {
            throw new BadRequestException("PRACTICE_TEST lesson must contain only test questions");
        }

        int pointsAwarded = 0;
        for (PracticeQuestion question : practiceLesson.getQuestions()) {
            if (!QuestionType.TEST_QUESTIONS.contains(question.getQuestionType())) {
                continue;
            }
            
            List<String> selectedAnswers = answersByQuestion.get(question.getQuestionIndex());

            List<String> correctAnswers = normalizeList(splitRaw(question.getCorrectAnswersRaw()));
            pointsAwarded += scoreQuestion(question, selectedAnswers, correctAnswers);
        }

        boolean passed = pointsAwarded * 100 >= practiceLesson.getFullPoints() * practiceLesson.getPassingThresholdPercent();

        submission.setAnswerRaw(serializeAnswersByQuestion(answersByQuestion));
        submission.setStatus(passed ? SubmissionStatus.COMPLETE : SubmissionStatus.INCOMPLETE);
        submission.setCompleted(passed);
        submission.setPointsAwarded(pointsAwarded);
        submission = submissionRepository.save(submission);
        if (passed) {
            markEnrollmentCompletedIfDone(studentId, lesson.getCourse().getId());
        }

        return new SubmissionResultDto(
                submission.getId(),
                submission.getStatus(),
                submission.getCompleted(),
                passed ? "Practice completed" : "Practice is not completed"
        );
    }

    @Transactional(readOnly = true)
    public List<PendingSubmissionDto> getPendingReviews() {
        Long adminId = securityUtils.currentUserId();
        final List<Long> courseIdsThatCanReview = courseService.findCoursesThatAdminCanReview(adminId).stream()
                .map(Course::getId).toList();
        final List<LessonSubmission> pendingOrReworkSubmissions = submissionRepository
                .findAllByStatusAndLessonCourseIdIn(SubmissionStatus.PENDING_REVIEW, courseIdsThatCanReview);

        return pendingOrReworkSubmissions.stream()
                .map(s -> new PendingSubmissionDto(
                        s.getId(),
                        s.getLesson().getId(),
                        s.getLesson().getTitle(),
                        s.getStudent().getId(),
                        s.getStudent().getEmail(),
                        s.getAnswerRaw(),
                        s.getSubmittedAt().toString()
                )).toList();
    }

    @Transactional
    public SubmissionResultDto reviewOpenSubmission(Long submissionId, ReviewOpenSubmissionRequest request) {
        Long reviewerId = securityUtils.currentUserId();
        LessonSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found: " + submissionId));

        SubmissionStatus previousStatus = submission.getStatus();

        if (submission.getStatus() != SubmissionStatus.PENDING_REVIEW) {
            throw new BadRequestException("Submission is not in reviewable status");
        }

        if (!courseService.canReviewCourse(submission.getLesson().getCourse().getId(), reviewerId)) {
            throw new AccessDeniedException("Admin is not assigned as reviewer for this course");
        }

        boolean finalPassed = request.passed();
        SubmissionStatus finalStatus = request.passed() ? SubmissionStatus.COMPLETE : SubmissionStatus.INCOMPLETE;
        if (request.toNextReview()) {
            finalStatus = SubmissionStatus.REWORK;
            finalPassed = false;
        }
        final int pointsAwarded = finalPassed
                ? request.partialPoints()
                    ? submission.getLesson().getPartialPoints()
                    : submission.getLesson().getFullPoints()
                : 0;

        submission.setCompleted(finalPassed);
        submission.setStatus(finalStatus);
        submission.setPointsAwarded(pointsAwarded);
        submission.setReviewComment(request.comment());
        submission.setReviewedByAdminId(reviewerId);
        submission.setReviewedAt(LocalDateTime.now());

        submission = submissionRepository.save(submission);
        if (finalPassed) {
            markEnrollmentCompletedIfDone(submission.getStudent().getId(), submission.getLesson().getCourse().getId());
        }

        businessEventLogger.log("learning.open_submission.review", "success",
                "submissionId", submission.getId(),
                "courseId", submission.getLesson().getCourse().getId(),
                "lessonId", submission.getLesson().getId(),
                "studentId", submission.getStudent().getId(),
                "reviewerId", reviewerId,
                "fromStatus", previousStatus,
                "toStatus", finalStatus,
                "passed", finalPassed,
                "points", pointsAwarded);

        return new SubmissionResultDto(
                submission.getId(),
                submission.getStatus(),
                submission.getCompleted(),
                "Submission reviewed"
        );
    }

    private void validateStudentEnrolled(Long userId, Long courseId) {
        if (!courseService.isUserEnrolled(userId, courseId)) {
            throw new BadRequestException("Student is not enrolled in this course");
        }
    }

    private void assertLessonAccessAllowed(Long studentId, Lesson lesson) {
        if (Boolean.TRUE.equals(lesson.getCourse().getLessonsFreeOrder())) {
            return;
        }

        if (lesson.getPosition() == null || lesson.getPosition() <= 1) {
            return;
        }

        Optional<Lesson> previousLesson = lessonRepository
                .findFirstByCourse_IdAndPositionLessThanOrderByPositionDesc(lesson.getCourse().getId(), lesson.getPosition());
        if (previousLesson.isEmpty()) {
            return;
        }

        boolean previousPassed = submissionRepository
                .findFirstByStudentIdAndLessonIdAndPassedTrueOrderBySubmittedAtDesc(studentId, previousLesson.get().getId())
                .isPresent();
        if (!previousPassed) {
            throw new BadRequestException("Previous lesson is not passed");
        }
    }

    private void assertStopLessonAccessAllowed(Long studentId, Lesson lesson) {
        if (lesson.getPosition() == null) {
            return;
        }

        boolean hasBlockingStopLesson = lessonRepository.existsUncompletedStopLessonBeforePosition(
                lesson.getCourse().getId(),
                studentId,
                lesson.getPosition()
        );
        if (hasBlockingStopLesson) {
            throw new BadRequestException("Previous stop lesson is not passed");
        }
    }

    private AppUser getStudent(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (user.getRole() != Role.STUDENT && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only STUDENT or ADMIN can submit lessons");
        }
        return user;
    }

    private boolean isTheoryLesson(Lesson lesson) {
        return lesson.getLessonType() == LessonType.THEORY_TEXT
                || lesson.getLessonType() == LessonType.THEORY_VIDEO
                || lesson.getLessonType() == LessonType.THEORY_PDF;
    }

    private LessonSubmission completeTheoryLessonInternal(Long studentId, Lesson lesson) {
        Optional<LessonSubmission> existingPassedTheorySubmission = submissionRepository
                .findFirstByStudentIdAndLessonIdAndPassedTrueOrderBySubmittedAtDesc(studentId, lesson.getId());
        if (existingPassedTheorySubmission.isPresent()) {
            return existingPassedTheorySubmission.get();
        }

        LessonSubmission submission = new LessonSubmission();
        submission.setLesson(lesson);
        submission.setStudent(getStudent(studentId));
        submission.setStatus(SubmissionStatus.COMPLETE);
        submission.setCompleted(true);
        submission.setPointsAwarded(lesson.getFullPoints());
        submission.setSubmittedAt(LocalDateTime.now());

        markEnrollmentStarted(studentId, lesson.getCourse().getId());
        submission = submissionRepository.save(submission);
        markEnrollmentCompletedIfDone(studentId, lesson.getCourse().getId());
        return submission;
    }

    private boolean evaluateCorrectness(QuestionType type, List<String> selected, List<String> correct) {
        if (type == QuestionType.ORDERING) {
            return Objects.equals(selected, correct);
        }
        return new HashSet<>(selected).equals(new HashSet<>(correct));
    }

    private int scoreQuestion(PracticeQuestion question, List<String> selectedAnswers, List<String> correctAnswers) {
        if (evaluateCorrectness(question.getQuestionType(), selectedAnswers, correctAnswers)) {
            return question.getFullPoints() == null ? 0 : question.getFullPoints();
        }

        if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE
                && new HashSet<>(correctAnswers).containsAll(selectedAnswers)
                && !selectedAnswers.isEmpty()) {
            return question.getPartialPoints() == null ? 0 : question.getPartialPoints();
        }

        return 0;
    }

    private Map<Integer, List<String>> validateAndNormalizeAnswersByQuestion(PracticeLesson lesson,
                                                                             PracticeSubmissionRequest request) {
        Map<Integer, List<String>> incoming = request.questionAnswers();
        if (incoming == null || incoming.isEmpty()) {
            throw new BadRequestException("questionAnswers is required");
        }

        if (lesson.getQuestions() == null || lesson.getQuestions().isEmpty()) {
            throw new BadRequestException("Practice lesson has no questions");
        }

        Set<Integer> lessonQuestionIndexes = lesson.getQuestions().stream()
                .map(PracticeQuestion::getQuestionIndex)
                .collect(Collectors.toSet());

        Set<Integer> incomingQuestionIndexes = incoming.keySet();
        if (!lessonQuestionIndexes.equals(incomingQuestionIndexes)) {
            throw new BadRequestException("questionAnswers must contain answers for all lesson questions and only for them");
        }

        Map<Integer, PracticeQuestion> questionsByIndex = lesson.getQuestions().stream()
                .collect(Collectors.toMap(PracticeQuestion::getQuestionIndex, q -> q));

        Map<Integer, List<String>> normalized = new HashMap<>();
        for (Integer questionIndex : lessonQuestionIndexes) {
            PracticeQuestion question = questionsByIndex.get(questionIndex);
            List<String> answers = normalizeList(incoming.get(questionIndex));
            if (answers.isEmpty()) {
                throw new BadRequestException("Each question must have a non-empty answer list");
            }

            if (question.getQuestionType() == QuestionType.OPEN_ANSWER) {
                if (answers.size() != 1) {
                    throw new BadRequestException("OPEN_ANSWER question must have exactly one answer");
                }
                normalized.put(questionIndex, answers);
                continue;
            }

            if (question.getQuestionType() == QuestionType.SINGLE_CHOICE && answers.size() != 1) {
                throw new BadRequestException("SINGLE_CHOICE question must have exactly one selected answer");
            }

            normalized.put(questionIndex, answers);
        }

        return normalized;
    }

    private String serializeAnswersByQuestion(Map<Integer, List<String>> answersByQuestion) {
        return answersByQuestion.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + String.join("|", normalizeList(e.getValue())))
                .reduce((left, right) -> left + ";;" + right)
                .orElse("");
    }

    private void markEnrollmentStarted(Long userId, Long courseId) {
        enrollmentRepository.findByUserIdAndCourseId(userId, courseId).ifPresent(e -> {
            if (e.getStartedAt() == null) {
                e.setStartedAt(LocalDateTime.now());
                enrollmentRepository.save(e);
                programService.onCourseProgressChanged(userId, courseId);
            }
        });
    }

    private void markEnrollmentCompletedIfDone(Long userId, Long courseId) {
        long passedLessons = submissionRepository.countDistinctPassedLessons(userId, courseId);
        long totalLessons = courseService.getCourseLessons(courseId).size();
        if (totalLessons > 0 && passedLessons >= totalLessons) {
            enrollmentRepository.findByUserIdAndCourseId(userId, courseId).ifPresent(e -> {
                if (e.getCompletedAt() == null) {
                    e.setCompletedAt(LocalDateTime.now());
                    enrollmentRepository.save(e);
                    programService.onCourseProgressChanged(userId, courseId);
                }
            });
        }
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private List<String> splitRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(";;", -1)).toList();
    }

    private void validatePracticeAttemptLimit(Long studentId, Long lessonId, Integer attemptLimit) {
        if (attemptLimit == null || attemptLimit <= 0) {
            return;
        }

        long attempts = submissionRepository.countByStudentIdAndLessonId(studentId, lessonId);
        if (attempts >= attemptLimit) {
            throw new BadRequestException("Attempt limit exceeded for this lesson");
        }
    }

    private void validatePracticeTimeLimit(Long studentId, Long lessonId, Integer timeLimitMinutes) {
        if (timeLimitMinutes == null || timeLimitMinutes <= 0) {
            return;
        }

        Optional<LessonSubmission> firstAttempt = submissionRepository
                .findFirstByStudentIdAndLessonIdOrderBySubmittedAtAsc(studentId, lessonId);
        if (firstAttempt.isEmpty()) {
            return;
        }

        LocalDateTime deadlineAt = firstAttempt.get().getSubmittedAt().plusMinutes(timeLimitMinutes);
        if (LocalDateTime.now().isAfter(deadlineAt)) {
            throw new BadRequestException("Time limit exceeded for this lesson");
        }
    }

    private boolean isLessonPassedByStudent(Long studentId, Long lessonId) {
        return submissionRepository
                .findFirstByStudentIdAndLessonIdAndPassedTrueOrderBySubmittedAtDesc(studentId, lessonId)
                .isPresent();
    }
}
