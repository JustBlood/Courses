package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
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
import ru.just.monolithmvp.repository.AppUserRepository;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LearningService {
    private final CourseService courseService;
    private final LessonSubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AppUserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final BusinessEventLogger businessEventLogger;

    public LearnerLessonDto getLessonForLearner(Long lessonId, Long userId) {
        final Lesson lesson = courseService.getLessonEntity(lessonId);
        validateStudentEnrolled(userId, lesson.getCourse().getId());

        LearnerLessonDto.LearnerLessonDtoBuilder learnerLessonDtoBuilder = LearnerLessonDto.builder()
                .id(lesson.getId())
                .position(lesson.getPosition())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .lessonType(lesson.getLessonType());
        if (LessonType.LessonSubType.PRACTICE.equals(lesson.getLessonType().getSubType())) {
            final List<PracticeQuestion> practiceQuestions = courseService.getPracticeQuestionForLesson(lessonId);
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

    @Transactional
    public SubmissionResultDto completeTheoryLesson(Long lessonId) {
        Long studentId = securityUtils.currentUserId();
        Lesson lesson = courseService.getLessonEntity(lessonId);
        validateStudentEnrolled(studentId, lesson.getCourse().getId());

        if (lesson.getLessonType() != LessonType.THEORY_TEXT
                && lesson.getLessonType() != LessonType.THEORY_VIDEO
                && lesson.getLessonType() != LessonType.THEORY_PDF) {
            throw new BadRequestException("Lesson is not THEORY");
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
        return new SubmissionResultDto(submission.getId(), submission.getStatus(), true, "Theory lesson completed");
    }

    @Transactional
    public SubmissionResultDto submitPractice(Long lessonId, PracticeSubmissionRequest request) {
        Long studentId = securityUtils.currentUserId();
        Lesson lesson = courseService.getLessonEntity(lessonId);
        validateStudentEnrolled(studentId, lesson.getCourse().getId());

        if (lesson instanceof PracticeLesson practiceLesson) {
            validatePracticeAttemptLimit(studentId, lessonId, practiceLesson.getAttemptLimit());
        }

        LessonSubmission submission = new LessonSubmission();
        submission.setLesson(lesson);
        submission.setStudent(getStudent(studentId));
        submission.setSubmittedAt(LocalDateTime.now());
        markEnrollmentStarted(studentId, lesson.getCourse().getId());

        if (!(lesson instanceof PracticeLesson practiceLesson)
                || (lesson.getLessonType() != LessonType.PRACTICE_TEST
                && lesson.getLessonType() != LessonType.PRACTICE_OPEN_ANSWER)) {
            throw new BadRequestException("Lesson is not PRACTICE");
        }

        if (lesson.getLessonType() == LessonType.PRACTICE_OPEN_ANSWER) {
            if (request.openAnswer() == null || request.openAnswer().isBlank()) {
                throw new BadRequestException("openAnswer is required for assignment task");
            }
            submission.setAnswerRaw(request.openAnswer());
            submission.setStatus(SubmissionStatus.PENDING_REVIEW);
            submission.setPassed(false);
            submission.setPointsAwarded(0);
            submission = submissionRepository.save(submission);
            return new SubmissionResultDto(
                    submission.getId(),
                    submission.getStatus(),
                    false,
                    "Answer submitted and waiting for admin review"
            );
        }

        Map<Integer, List<String>> answersByQuestion = resolveAnswersByQuestion(practiceLesson, request);

        int pointsAwarded = 0;
        int maxPoints = 0;
        for (PracticeQuestion question : practiceLesson.getQuestions()) {
            if (!QuestionType.TEST_QUESTIONS.contains(question.getQuestionType())) {
                continue;
            }

            maxPoints += question.getFullPoints() == null ? 0 : question.getFullPoints();
            List<String> selectedAnswers = normalizeList(answersByQuestion.get(question.getQuestionIndex()));
            if (selectedAnswers.isEmpty()) {
                continue;
            }

            List<String> correctAnswers = normalizeList(splitRaw(question.getCorrectAnswersRaw()));
            pointsAwarded += scoreQuestion(question, selectedAnswers, correctAnswers);
        }

        boolean passed = maxPoints > 0
                && pointsAwarded * 100 >= maxPoints * practiceLesson.getPassingThresholdPercent();

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
        final List<LessonSubmission> pendingOrReworkSubmissions = new ArrayList<>();
        pendingOrReworkSubmissions.addAll(
                submissionRepository.findAllByStatusAndLessonCourseIdIn(SubmissionStatus.PENDING_REVIEW, courseIdsThatCanReview)
        );
        pendingOrReworkSubmissions.addAll(
                submissionRepository.findAllByStatusAndLessonCourseIdIn(SubmissionStatus.REWORK, courseIdsThatCanReview)
        );

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

        if (submission.getStatus() != SubmissionStatus.PENDING_REVIEW
                && submission.getStatus() != SubmissionStatus.REWORK) {
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

    private AppUser getStudent(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (user.getRole() != Role.STUDENT && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only STUDENT or ADMIN can submit lessons");
        }
        return user;
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
                && selectedAnswers.stream().allMatch(correctAnswers::contains)
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
}
