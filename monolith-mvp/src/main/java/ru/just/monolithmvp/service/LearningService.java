package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.learning.*;
import ru.just.monolithmvp.dto.lesson.LearnerLessonDto;
import ru.just.monolithmvp.dto.lesson.LearnerPracticeQuestionDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.observability.BusinessEventLogger;
import ru.just.monolithmvp.repository.AppUserRepository;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.LessonRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
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
            final PracticeLesson practiceLesson = (PracticeLesson) lesson;
            final LessonSubmission submission = submissionRepository.findByStudentIdAndLessonId(userId, lessonId)
                    .orElse(null);
            final Map<Integer, QuestionProgress> questionProgressByIndex = Optional.ofNullable(submission)
                    .map(LessonSubmission::getQuestionProgress)
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(QuestionProgress::getQuestionIndex, q -> q, (left, right) -> right));
            final boolean showQuestionStatus = Boolean.TRUE.equals(practiceLesson.getShowQuestionStatus());
            final boolean showCorrectAnswers = Boolean.TRUE.equals(practiceLesson.getShowCorrectAnswersAfterCompletion())
                    && submission != null
                    && Boolean.TRUE.equals(submission.getCompleted());

            final List<PracticeQuestion> practiceQuestions = selectPracticeQuestionsForAttempt(practiceLesson);
            final List<LearnerPracticeQuestionDto> questions = practiceQuestions.stream().map(question -> {
                final QuestionProgress questionProgress = questionProgressByIndex.get(question.getQuestionIndex());
                return new LearnerPracticeQuestionDto(
                        question.getQuestionIndex(),
                        question.getQuestionType(),
                        question.getQuestionText(),
                        Optional.ofNullable(question.getOptions()).orElse(List.of()),
                        questionProgress != null ? questionProgress.getAnswers() : List.of(),
                        showCorrectAnswers && question.getCorrectAnswers() != null && !question.getCorrectAnswers().isEmpty()
                                ? question.getCorrectAnswers()
                                : null,
                        showQuestionStatus
                                ? Optional.ofNullable(questionProgress)
                                .map(QuestionProgress::getReviewStatus)
                                .orElse(null)
                                : null,
                        showQuestionStatus
                                ? Optional.ofNullable(questionProgress)
                                .map(QuestionProgress::getAwardedPoints)
                                .orElse(null)
                                : null,
                        question.getFullPoints(),
                        question.getPartialPoints()
                );
            }).toList();
            learnerLessonDtoBuilder.questions(questions);
        } else {
            TheoryLesson theoryLesson = (TheoryLesson) lesson;
            learnerLessonDtoBuilder
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

        LessonSubmission existingSubmission = submissionRepository
                .findByStudentIdAndLessonId(studentId, lessonId)
                .orElse(null);
        if (existingSubmission != null && Boolean.TRUE.equals(existingSubmission.getCompleted())) {
            throw new BadRequestException("Submission is already finalized");
        }

        LessonSubmission submission;
        markEnrollmentStarted(studentId, lesson.getCourse().getId());

        Map<Integer, List<String>> answersByQuestion = validateAndNormalizeAnswersByQuestion(practiceLesson, request);

        if (lesson.getLessonType() == LessonType.PRACTICE_OPEN_ANSWER) {
            if (practiceLesson.getQuestions().stream().anyMatch(q -> q.getQuestionType() != QuestionType.OPEN_ANSWER)) {
                throw new BadRequestException("PRACTICE_OPEN_ANSWER lesson must contain only OPEN_ANSWER questions");
            }

            boolean isReworkSubmission = existingSubmission != null && existingSubmission.getStatus() == SubmissionStatus.REWORK;
            if (existingSubmission != null && !isReworkSubmission) {
                throw new BadRequestException("Open submission can be updated only from REWORK status");
            }

            if (existingSubmission != null) {
                submission = existingSubmission;
            } else {
                submission = new LessonSubmission();
                submission.setLesson(lesson);
                final AppUser student = new AppUser();
                student.setId(studentId);
                submission.setStudent(student);
            }

            submission.setQuestionProgress(buildOpenQuestionProgressForSubmit(
                    practiceLesson,
                    answersByQuestion,
                    submission.getQuestionProgress(),
                    isReworkSubmission
            ));
            submission.setStatus(SubmissionStatus.PENDING_REVIEW);
            submission.setCompleted(false);
            submission.setPointsAwarded(0);
            submission.setAttemptCounter(Optional.ofNullable(submission.getAttemptCounter()).orElse(0) + 1);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setReviewedByAdminId(null);
            submission.setReviewedAt(null);

            submission = submissionRepository.save(submission);
            return new SubmissionResultDto(
                    submission.getId(),
                    submission.getStatus(),
                    false,
                    "Answer submitted and waiting for admin review"
            );
        }

        if (existingSubmission != null) {
            submission = existingSubmission;
        } else {
            submission = new LessonSubmission();
            submission.setLesson(lesson);
            submission.setStudent(getStudent(studentId));
            submission.setSubmittedAt(LocalDateTime.now());
        }

        if (practiceLesson.getQuestions().stream().anyMatch(q -> q.getQuestionType() == QuestionType.OPEN_ANSWER)) {
            throw new BadRequestException("PRACTICE_TEST lesson must contain only test questions");
        }

        int totalQuestionPoints = 0;
        for (PracticeQuestion question : practiceLesson.getQuestions()) {
            if (!QuestionType.TEST_QUESTIONS.contains(question.getQuestionType())) {
                continue;
            }
            
            List<String> selectedAnswers = answersByQuestion.get(question.getQuestionIndex());

            List<String> correctAnswers = normalizeList(question.getCorrectAnswers());
            totalQuestionPoints += scoreQuestion(question, selectedAnswers, correctAnswers);
        }

        final Integer maxPointsByAllQuestions = practiceLesson.getQuestions().stream().map(PracticeQuestion::getFullPoints).reduce(Integer::sum).get();
        boolean passed = totalQuestionPoints * 100 >= maxPointsByAllQuestions * practiceLesson.getPassingThresholdPercent();
        int lessonPointsAwarded = passed ? practiceLesson.getFullPoints() : 0;

        submission.setQuestionProgress(buildTestQuestionProgress(practiceLesson, answersByQuestion));
        submission.setStatus(passed ? SubmissionStatus.COMPLETE : SubmissionStatus.INCOMPLETE);
        submission.setCompleted(passed);
        submission.setPointsAwarded(lessonPointsAwarded);
        submission.setAttemptCounter(Optional.ofNullable(submission.getAttemptCounter()).orElse(0) + 1);
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
        final List<LessonSubmission> pendingSubmissions = submissionRepository
                .findAllByStatusAndLessonCourseIdIn(SubmissionStatus.PENDING_REVIEW, courseIdsThatCanReview);

        return pendingSubmissions.stream()
                .map(s -> new PendingSubmissionDto(
                        s.getId(),
                        s.getLesson().getId(),
                        s.getLesson().getTitle(),
                        s.getLesson().getCourse().getId(),
                        s.getLesson().getCourse().getTitle(),
                        s.getStudent().getId(),
                        s.getStudent().getFullName(),
                        s.getSubmittedAt(),
                        Math.max(Optional.ofNullable(s.getAttemptCounter()).orElse(0), 1)
                )).toList();
    }

    @Transactional(readOnly = true)
    public List<PendingSubmissionQuestionDto> getPendingReviewQuestions(Long submissionId) {
        Long reviewerId = securityUtils.currentUserId();
        LessonSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found: " + submissionId));

        final PracticeLesson practiceLesson = validateAndGetPracticeLesson(submission, reviewerId);

        Map<Integer, QuestionProgress> progressByQuestionIndex = Optional.ofNullable(submission.getQuestionProgress())
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(QuestionProgress::getQuestionIndex, q -> q, (left, right) -> right));

        return practiceLesson.getQuestions().stream()
                .sorted(Comparator.comparing(PracticeQuestion::getQuestionIndex))
                .map(question -> {
                    QuestionProgress questionProgress = progressByQuestionIndex.get(question.getQuestionIndex());
                    OpenReviewStatus reviewStatus = questionProgress != null && questionProgress.getReviewStatus() != null
                            ? questionProgress.getReviewStatus()
                            : OpenReviewStatus.PENDING_REVIEW;
                    Integer awardedPoints = questionProgress != null && questionProgress.getAwardedPoints() != null
                            ? questionProgress.getAwardedPoints()
                            : 0;
                    List<String> answers = questionProgress != null && questionProgress.getAnswers() != null
                            ? questionProgress.getAnswers()
                            : List.of();

                    return new PendingSubmissionQuestionDto(
                            question.getQuestionIndex(),
                            reviewStatus,
                            question.getQuestionText(),
                            question.getTrainerHint(),
                            awardedPoints,
                            question.getFullPoints(),
                            answers.isEmpty() ? null : answers.getFirst(),
                            questionProgress == null ? null : questionProgress.getReviewComment()
                    );
                }).toList();
    }

    private PracticeLesson validateAndGetPracticeLesson(LessonSubmission submission, Long reviewerId) {
        if (!courseService.canReviewCourse(submission.getLesson().getCourse().getId(), reviewerId)) {
            throw new AccessDeniedException("Admin is not assigned as reviewer for this course");
        }
        if (Boolean.TRUE.equals(submission.getCompleted())) {
            throw new BadRequestException("Submission is already finalized");
        }
        if (submission.getStatus() != SubmissionStatus.PENDING_REVIEW && submission.getStatus() != SubmissionStatus.REWORK) {
            throw new BadRequestException("Submission is not in reviewable status");
        }

        return lessonRepository.findPracticeLessonById(submission.getLesson().getId())
                .orElseThrow(() -> new BadRequestException("Submission is not OPEN_ANSWER practice lesson"));
    }

    @Transactional
    public SubmissionResultDto reviewOpenSubmission(Long submissionId, ReviewOpenSubmissionRequest request) {
        Long reviewerId = securityUtils.currentUserId();
        LessonSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found: " + submissionId));

        SubmissionStatus previousStatus = submission.getStatus();

        final PracticeLesson practiceLesson = validateSubmissionForReviewAndGetPracticeLesson(submission, reviewerId);

        Map<Integer, ReviewQuestionDecisionDto> questionReviews = request.questionReviews();
        if (questionReviews == null || questionReviews.isEmpty()) {
            throw new BadRequestException("questionReviews is required");
        }

        Set<Integer> lessonQuestionIndexes = practiceLesson.getQuestions().stream()
                .map(PracticeQuestion::getQuestionIndex)
                .collect(Collectors.toSet());
        if (!lessonQuestionIndexes.equals(questionReviews.keySet())) {
            throw new BadRequestException("questionReviews must contain decisions for all lesson questions and only for them");
        }

        Map<Integer, QuestionProgress> existingProgressByQuestionIndex = Optional.ofNullable(submission.getQuestionProgress())
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(QuestionProgress::getQuestionIndex, q -> q, (left, right) -> right));

        boolean hasRework = false;
        int totalAwardedPoints = 0;
        List<QuestionProgress> nextProgress = new ArrayList<>();

        for (PracticeQuestion question : practiceLesson.getQuestions()) {
            Integer questionIndex = question.getQuestionIndex();
            ReviewQuestionDecisionDto decision = questionReviews.get(questionIndex);
            if (decision == null || decision.submissionStatus() == null) {
                throw new BadRequestException("submissionStatus is required for questionIndex=" + questionIndex);
            }

            OpenReviewStatus reviewStatus = decision.submissionStatus();
            if (reviewStatus == OpenReviewStatus.PENDING_REVIEW) {
                throw new BadRequestException("PENDING_REVIEW is not allowed as review decision");
            }

            int awardedPoints = Optional.ofNullable(decision.awardedPoints()).orElse(0);
            if ((reviewStatus == OpenReviewStatus.REWORK || reviewStatus == OpenReviewStatus.REJECTED) && awardedPoints != 0) {
                awardedPoints = 0;
            }
            if (reviewStatus == OpenReviewStatus.ACCEPTED) {
                if (awardedPoints < 0 || awardedPoints > question.getFullPoints()) {
                    throw new BadRequestException("ACCEPTED awardedPoints must be in range 0..fullPoints");
                }
                totalAwardedPoints += awardedPoints;
            }
            if (reviewStatus == OpenReviewStatus.REWORK) {
                hasRework = true;
            }

            QuestionProgress questionProgress = existingProgressByQuestionIndex.getOrDefault(questionIndex, new QuestionProgress());
            questionProgress.setQuestionIndex(questionIndex);
            questionProgress.setAnswers(Optional.ofNullable(questionProgress.getAnswers()).orElse(List.of()));
            questionProgress.setReviewStatus(reviewStatus);
            questionProgress.setAwardedPoints(awardedPoints);
            questionProgress.setPointsType(resolveOpenQuestionPointsType(awardedPoints, question.getFullPoints()));
            questionProgress.setReviewComment(decision.reviewComment());
            nextProgress.add(questionProgress);
        }

        final Integer maxPointsByAllQuestions = practiceLesson.getQuestions().stream().map(PracticeQuestion::getFullPoints).reduce(Integer::sum).get();
        boolean finalPassed = !hasRework
                && totalAwardedPoints * 100 >= maxPointsByAllQuestions * practiceLesson.getPassingThresholdPercent();
        SubmissionStatus finalStatus = hasRework
                ? SubmissionStatus.REWORK
                : (finalPassed ? SubmissionStatus.COMPLETE : SubmissionStatus.INCOMPLETE);
        int pointsAwarded = hasRework ? 0 : (finalPassed ? practiceLesson.getFullPoints() : 0);

        submission.setCompleted(!hasRework);
        submission.setStatus(finalStatus);
        submission.setPointsAwarded(pointsAwarded);
        submission.setQuestionProgress(nextProgress);
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

    private PracticeLesson validateSubmissionForReviewAndGetPracticeLesson(LessonSubmission submission, Long reviewerId) {
        if (Boolean.TRUE.equals(submission.getCompleted())) {
            throw new BadRequestException("Submission is already finalized");
        }
        if (submission.getStatus() != SubmissionStatus.PENDING_REVIEW && submission.getStatus() != SubmissionStatus.REWORK) {
            throw new BadRequestException("Submission is not in reviewable status");
        }

        if (!courseService.canReviewCourse(submission.getLesson().getCourse().getId(), reviewerId)) {
            throw new AccessDeniedException("Admin is not assigned as reviewer for this course");
        }

        return lessonRepository.findPracticeLessonById(submission.getLesson().getId())
                .orElseThrow(() -> new BadRequestException("Submission is not OPEN_ANSWER practice lesson"));
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
                .findFirstByStudentIdAndLessonIdAndCompletedTrueOrderBySubmittedAtDesc(studentId, previousLesson.get().getId())
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
                .findFirstByStudentIdAndLessonIdAndCompletedTrueOrderBySubmittedAtDesc(studentId, lesson.getId());
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
        return switch (resolveTestQuestionPointsType(question, selectedAnswers, correctAnswers)) {
            case FULL -> question.getFullPoints() == null ? 0 : question.getFullPoints();
            case PARTIAL -> question.getPartialPoints() == null ? 0 : question.getPartialPoints();
            case ZERO -> 0;
        };
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
        long passedLessons = submissionRepository.countDistinctCompletedLessons(userId, courseId);
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

    private void validatePracticeAttemptLimit(Long studentId, Long lessonId, Integer attemptLimit) {
        if (attemptLimit == null || attemptLimit <= 0) {
            return;
        }

        long attempts = submissionRepository.findByStudentIdAndLessonId(studentId, lessonId)
                .map(submission -> (long) Optional.ofNullable(submission.getAttemptCounter()).orElse(0))
                .orElse(0L);
        if (attempts >= attemptLimit) {
            throw new BadRequestException("Attempt limit exceeded for this lesson");
        }
    }

    private void validatePracticeTimeLimit(Long studentId, Long lessonId, Integer timeLimitMinutes) {
        if (timeLimitMinutes == null || timeLimitMinutes <= 0) {
            return;
        }

        Optional<LessonSubmission> firstAttempt = submissionRepository
                .findByStudentIdAndLessonId(studentId, lessonId);
        if (firstAttempt.isEmpty()) {
            return;
        }

        LocalDateTime firstSubmittedAt = firstAttempt.get().getSubmittedAt();
        if (firstSubmittedAt == null) {
            return;
        }

        LocalDateTime deadlineAt = firstSubmittedAt.plusMinutes(timeLimitMinutes);
        if (LocalDateTime.now().isAfter(deadlineAt)) {
            throw new BadRequestException("Time limit exceeded for this lesson");
        }
    }

    private boolean isLessonPassedByStudent(Long studentId, Long lessonId) {
        return submissionRepository
                .findFirstByStudentIdAndLessonIdAndCompletedTrueOrderBySubmittedAtDesc(studentId, lessonId)
                .isPresent();
    }

    private List<QuestionProgress> buildOpenQuestionProgressForSubmit(PracticeLesson lesson,
                                                                      Map<Integer, List<String>> answersByQuestion,
                                                                      List<QuestionProgress> existingProgress,
                                                                      boolean reworkSubmission) {
        Map<Integer, QuestionProgress> existingProgressByQuestionIndex = Optional.ofNullable(existingProgress)
                .orElse(List.of())
                .stream()
                .filter(q -> q.getQuestionIndex() != null)
                .collect(Collectors.toMap(QuestionProgress::getQuestionIndex, q -> q, (left, right) -> right));

        return lesson.getQuestions().stream()
                .sorted(Comparator.comparing(PracticeQuestion::getQuestionIndex))
                .map(question -> {
                    Integer questionIndex = question.getQuestionIndex();
                    QuestionProgress progress = existingProgressByQuestionIndex.getOrDefault(questionIndex, new QuestionProgress());
                    progress.setQuestionIndex(questionIndex);

                    OpenReviewStatus currentReviewStatus = progress.getReviewStatus();
                    boolean shouldResetToPending = !reworkSubmission
                            || currentReviewStatus == null
                            || currentReviewStatus == OpenReviewStatus.PENDING_REVIEW
                            || currentReviewStatus == OpenReviewStatus.REWORK;

                    if (shouldResetToPending) {
                        progress.setAnswers(answersByQuestion.getOrDefault(questionIndex, List.of()));
                        progress.setReviewStatus(OpenReviewStatus.PENDING_REVIEW);
                        progress.setAwardedPoints(0);
                        progress.setPointsType(QuestionPointsType.ZERO);
                    } else if (progress.getAnswers() == null) {
                        progress.setAnswers(answersByQuestion.getOrDefault(questionIndex, List.of()));
                    }

                    return progress;
                }).toList();
    }

    private List<QuestionProgress> buildTestQuestionProgress(PracticeLesson lesson,
                                                             Map<Integer, List<String>> answersByQuestion) {
        return lesson.getQuestions().stream()
                .sorted(Comparator.comparing(PracticeQuestion::getQuestionIndex))
                .filter(question -> QuestionType.TEST_QUESTIONS.contains(question.getQuestionType()))
                .map(question -> {
                    List<String> selectedAnswers = answersByQuestion.getOrDefault(question.getQuestionIndex(), List.of());
                    List<String> correctAnswers = normalizeList(question.getCorrectAnswers());
                    QuestionPointsType pointsType = resolveTestQuestionPointsType(question, selectedAnswers, correctAnswers);
                    int awardedPoints = switch (pointsType) {
                        case FULL -> question.getFullPoints() == null ? 0 : question.getFullPoints();
                        case PARTIAL -> question.getPartialPoints() == null ? 0 : question.getPartialPoints();
                        case ZERO -> 0;
                    };

                    return QuestionProgress.builder()
                            .questionIndex(question.getQuestionIndex())
                            .answers(selectedAnswers)
                            .pointsType(pointsType)
                            .awardedPoints(awardedPoints)
                            .reviewStatus(null)
                            .reviewComment(null)
                            .build();
                }).toList();
    }

    private QuestionPointsType resolveTestQuestionPointsType(PracticeQuestion question,
                                                             List<String> selectedAnswers,
                                                             List<String> correctAnswers) {
        if (evaluateCorrectness(question.getQuestionType(), selectedAnswers, correctAnswers)) {
            return QuestionPointsType.FULL;
        }

        if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            Set<String> selectedSet = new HashSet<>(selectedAnswers);
            Set<String> correctSet = new HashSet<>(correctAnswers);

            long wrongSelected = selectedSet.stream().filter(answer -> !correctSet.contains(answer)).count();
            long missedCorrect = correctSet.stream().filter(answer -> !selectedSet.contains(answer)).count();

            if (wrongSelected <= 1 && missedCorrect <= 1 && Optional.ofNullable(question.getOptions()).orElse(List.of()).size() != 3) {
                return QuestionPointsType.PARTIAL;
            }
        }

        return QuestionPointsType.ZERO;
    }

    private QuestionPointsType resolveOpenQuestionPointsType(int awardedPoints, int fullPoints) {
        if (awardedPoints <= 0) {
            return QuestionPointsType.ZERO;
        }
        if (awardedPoints >= fullPoints) {
            return QuestionPointsType.FULL;
        }
        return QuestionPointsType.PARTIAL;
    }
}
