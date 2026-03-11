package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.learning.*;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.observability.BusinessEventLogger;
import ru.just.monolithmvp.repository.LessonRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenReviewService {
    private final CourseAssignmentService courseAssignmentService;
    private final LessonRepository lessonRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final SecurityUtils securityUtils;
    private final BusinessEventLogger businessEventLogger;
    private final EnrollmentProgressService enrollmentProgressService;
    private final PracticeScoringPolicy practiceScoringPolicy;

    @Transactional(readOnly = true)
    public List<PendingSubmissionDto> getPendingReviews() {
        Long adminId = securityUtils.currentUserId();
        final List<Long> courseIdsThatCanReview = courseAssignmentService.findCoursesThatAdminCanReview(adminId).stream()
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
            questionProgress.setPointsType(practiceScoringPolicy.resolveOpenQuestionPointsType(awardedPoints, question.getFullPoints()));
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
            enrollmentProgressService.markEnrollmentCompletedIfDone(submission.getStudent().getId(), submission.getLesson().getCourse().getId());
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

    private PracticeLesson validateAndGetPracticeLesson(LessonSubmission submission, Long reviewerId) {
        if (!courseAssignmentService.canReviewCourse(submission.getLesson().getCourse().getId(), reviewerId)) {
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

    private PracticeLesson validateSubmissionForReviewAndGetPracticeLesson(LessonSubmission submission, Long reviewerId) {
        if (Boolean.TRUE.equals(submission.getCompleted())) {
            throw new BadRequestException("Submission is already finalized");
        }
        if (submission.getStatus() != SubmissionStatus.PENDING_REVIEW && submission.getStatus() != SubmissionStatus.REWORK) {
            throw new BadRequestException("Submission is not in reviewable status");
        }

        if (!courseAssignmentService.canReviewCourse(submission.getLesson().getCourse().getId(), reviewerId)) {
            throw new AccessDeniedException("Admin is not assigned as reviewer for this course");
        }

        return lessonRepository.findPracticeLessonById(submission.getLesson().getId())
                .orElseThrow(() -> new BadRequestException("Submission is not OPEN_ANSWER practice lesson"));
    }
}
