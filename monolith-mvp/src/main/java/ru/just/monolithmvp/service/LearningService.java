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

@Service
@RequiredArgsConstructor
public class LearningService {
    private final CourseService courseService;
    private final LessonRepository lessonRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AppUserRepository userRepository;
    private final LessonSubmissionStatusHistoryRepository submissionStatusHistoryRepository;
    private final SecurityUtils securityUtils;
    private final BusinessEventLogger businessEventLogger;
    private final ProgramService programService;

    @Transactional
    public LearnerLessonDto getLessonForLearner(Long lessonId, Long userId) {
        final Lesson lesson = courseService.getLessonEntity(lessonId);
        validateStudentEnrolled(userId, lesson.getCourse().getId());
        assertLessonAccessAllowed(userId, lesson);
        assertStopLessonAccessAllowed(userId, lesson);
        courseService.assertCourseDeadlineNotExceededForStudent(userId, lesson.getCourse().getId());

        if (isTheoryLesson(lesson)) {
            completeTheoryLessonInternal(userId, lesson);
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

        if (lesson.getLessonType() == LessonType.PRACTICE_OPEN_ANSWER) {
            if (request.openAnswer() == null || request.openAnswer().isBlank()) {
                throw new BadRequestException("openAnswer is required for assignment task");
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

            submission.setAnswerRaw(request.openAnswer());
            submission.setStatus(SubmissionStatus.PENDING_REVIEW);
            submission.setPassed(false);
            submission.setPointsAwarded(0);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setReviewedByAdminId(null);
            submission.setReviewedAt(null);
            submission.setReviewComment(null);

            submission = submissionRepository.save(submission);
            saveSubmissionStatusHistory(submission, previousStatus, SubmissionStatus.PENDING_REVIEW, null, null);
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

        Map<Integer, List<String>> answersByQuestion = resolveAnswersByQuestion(practiceLesson, request);

        int pointsAwarded = 0;
        for (PracticeQuestion question : practiceLesson.getQuestions()) {
            if (!QuestionType.TEST_QUESTIONS.contains(question.getQuestionType())) {
                continue;
            }
            
            List<String> selectedAnswers = normalizeList(answersByQuestion.get(question.getQuestionIndex()));
            if (selectedAnswers.isEmpty()) {
                continue;
            }

            List<String> correctAnswers = normalizeList(splitRaw(question.getCorrectAnswersRaw()));
            pointsAwarded += scoreQuestion(question, selectedAnswers, correctAnswers);
        }

        boolean passed = pointsAwarded * 100 >= practiceLesson.getFullPoints() * practiceLesson.getPassingThresholdPercent();

        submission.setAnswerRaw(serializeAnswersByQuestion(answersByQuestion));
        submission.setStatus(passed ? SubmissionStatus.COMPLETE : SubmissionStatus.INCOMPLETE);
        submission.setPassed(passed);
        submission.setPointsAwarded(pointsAwarded);
        submission = submissionRepository.save(submission);
        if (passed) {
            markEnrollmentCompletedIfDone(studentId, lesson.getCourse().getId());
        }

        return new SubmissionResultDto(
                submission.getId(),
                submission.getStatus(),
                submission.getPassed(),
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
        LessonSubmission submission = submissionRepository.findWithLockingById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found: " + submissionId));

        SubmissionStatus previousStatus = submission.getStatus();

        if (submission.getStatus() != SubmissionStatus.PENDING_REVIEW) {
            throw new BadRequestException("Submission is not in reviewable status");
        }

        if (!courseService.canReviewCourse(submission.getLesson().getCourse().getId(), reviewerId)) {
            throw new AccessDeniedException("Admin is not assigned as reviewer for this course");
        }

        boolean finalPassed = request.passed();
        SubmissionStatus finalStatus = request.passed() ? SubmissionStatus.ACCEPTED : SubmissionStatus.INCOMPLETE;
        if (request.toNextReview()) {
            finalStatus = SubmissionStatus.REWORK;
            finalPassed = false;
        }
        final int pointsAwarded = finalPassed
                ? request.partialPoints()
                    ? submission.getLesson().getPartialPoints()
                    : submission.getLesson().getFullPoints()
                : 0;

        submission.setPassed(finalPassed);
        submission.setStatus(finalStatus);
        submission.setPointsAwarded(pointsAwarded);
        submission.setReviewComment(request.comment());
        submission.setReviewedByAdminId(reviewerId);
        submission.setReviewedAt(LocalDateTime.now());

        submission = submissionRepository.save(submission);
        saveSubmissionStatusHistory(submission, previousStatus, finalStatus, reviewerId, request.comment());
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
                submission.getPassed(),
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

        boolean hasBlockingStopLesson = lessonRepository.existsUnpassedStopLessonBeforePosition(
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
        submission.setPassed(true);
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
                && correctAnswers.containsAll(selectedAnswers)
                && !selectedAnswers.isEmpty()) {
            return question.getPartialPoints() == null ? 0 : question.getPartialPoints();
        }

        return 0;
    }

    private Map<Integer, List<String>> resolveAnswersByQuestion(PracticeLesson lesson, PracticeSubmissionRequest request) {
        if (request.questionAnswers() != null && !request.questionAnswers().isEmpty()) {
            return request.questionAnswers();
        }

        if (request.selectedAnswers() == null || request.selectedAnswers().isEmpty()) {
            throw new BadRequestException("selectedAnswers or questionAnswers is required for choice tasks");
        }

        PracticeQuestion firstQuestion = lesson.getQuestions().stream()
                .filter(q -> QuestionType.TEST_QUESTIONS.contains(q.getQuestionType()))
                .min(Comparator.comparing(PracticeQuestion::getQuestionIndex))
                .orElseThrow(() -> new BadRequestException("Practice lesson has no test questions"));

        return Map.of(firstQuestion.getQuestionIndex(), request.selectedAnswers());
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

    private void saveSubmissionStatusHistory(LessonSubmission submission,
                                             SubmissionStatus fromStatus,
                                             SubmissionStatus toStatus,
                                             Long changedByAdminId,
                                             String comment) {
        LessonSubmissionStatusHistory history = new LessonSubmissionStatusHistory();
        history.setSubmission(submission);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedByAdminId(changedByAdminId);
        history.setChangedAt(LocalDateTime.now());
        history.setComment(comment);
        submissionStatusHistoryRepository.save(history);
    }
}
