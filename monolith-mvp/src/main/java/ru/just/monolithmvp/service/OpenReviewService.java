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

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final CourseProgressService courseProgressService;
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
                        s.getSubmittedAt().toEpochSecond(ZoneOffset.UTC),
                        Math.max(Optional.ofNullable(s.getAttemptCounter()).orElse(0), 1)
                )).toList();
    }

    @Transactional(readOnly = true)
    public List<PendingSubmissionQuestionDto> getPendingReviewQuestions(Long submissionId) {
        Long reviewerId = securityUtils.currentUserId();
        LessonSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found: " + submissionId));

        final PracticeLesson practiceLesson = validateAndGetPracticeLesson(submission, reviewerId);

        Map<Long, QuestionProgress> progressByQuestionId = Optional.ofNullable(submission.getQuestionProgress())
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(QuestionProgress::getQuestionId, q -> q, (left, right) -> right));

        return practiceLesson.getQuestions().stream()
                .sorted(Comparator.comparing(PracticeQuestion::getQuestionIndex))
                .map(question -> {
                    QuestionProgress questionProgress = progressByQuestionId.get(question.getId());
                    OpenReviewStatus reviewStatus = questionProgress != null && questionProgress.getReviewStatus() != null
                            ? questionProgress.getReviewStatus()
                            : OpenReviewStatus.PENDING_REVIEW;
                    Integer awardedPoints = questionProgress != null && questionProgress.getPointsType() != null
                            ? practiceScoringPolicy.scoreQuestion(questionProgress.getPointsType(), question)
                            : null;
                    List<String> answers = questionProgress != null && questionProgress.getAnswers() != null
                            ? questionProgress.getAnswers()
                            : List.of();

                    return new PendingSubmissionQuestionDto(
                            question.getId(),
                            question.getQuestionIndex(),
                            reviewStatus,
                            question.getQuestionText(),
                            question.getTrainerHint(),
                            awardedPoints,
                            question.getFullPoints(),
                            question.getPartialPoints(),
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

        Map<Long, ReviewQuestionDecisionDto> questionReviews = request.questionReviews();
        if (questionReviews == null || questionReviews.isEmpty()) {
            throw new BadRequestException("questionReviews is required");
        }

        Set<Long> lessonQuestionIndexes = practiceLesson.getQuestions().stream()
                .map(PracticeQuestion::getId)
                .collect(Collectors.toSet());
        if (!lessonQuestionIndexes.equals(questionReviews.keySet())) {
            throw new BadRequestException("questionReviews must contain decisions for all lesson questions and only for them");
        }

        Map<Long, QuestionProgress> existingProgressByQuestionId = Optional.ofNullable(submission.getQuestionProgress())
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(QuestionProgress::getQuestionId, q -> q, (left, right) -> right));

        boolean hasRework = false;
        int totalAwardedPoints = 0;
        List<QuestionProgress> nextProgress = new ArrayList<>();

        for (PracticeQuestion question : practiceLesson.getQuestions()) {
            ReviewQuestionDecisionDto decision = questionReviews.get(question.getId());
            OpenReviewStatus reviewStatus = decision.submissionStatus();
            validateReviewQuestionDecision(reviewStatus, decision, question.getId());

            int awardedPoints = decision.pointsType() != null
                    ? practiceScoringPolicy.scoreQuestion(decision.pointsType(), question)
                    : 0;
            totalAwardedPoints += awardedPoints;
            if (reviewStatus == OpenReviewStatus.REWORK) {
                hasRework = true;
            }

            QuestionProgress questionProgress = existingProgressByQuestionId.getOrDefault(question.getId(), new QuestionProgress());
            questionProgress.setQuestionId(question.getId());
            questionProgress.setAnswers(Optional.ofNullable(questionProgress.getAnswers()).orElse(List.of()));
            questionProgress.setReviewStatus(reviewStatus);
            questionProgress.setPointsType(decision.pointsType());
            questionProgress.setReviewComment(decision.reviewComment());
            nextProgress.add(questionProgress);
        }

        boolean finalPassed = !hasRework && practiceLesson.passedByPoints(totalAwardedPoints);
        SubmissionStatus finalStatus = hasRework
                ? SubmissionStatus.REWORKING
                : (finalPassed ? SubmissionStatus.COMPLETED : SubmissionStatus.INCOMPLETED);

        submission.setStatus(finalStatus);
        submission.setQuestionProgress(nextProgress);
        submission.setReviewedByAdminId(reviewerId);
        submission.setReviewedAt(LocalDateTime.now(Clock.systemUTC()));

        submission = submissionRepository.save(submission);
        courseProgressService.recalcCourseProgressByUser(submission.getStudent().getId(), submission.getLesson().getCourse().getId());

        businessEventLogger.log("learning.open_submission.review", "success",
                "submissionId", submission.getId(),
                "courseId", submission.getLesson().getCourse().getId(),
                "lessonId", submission.getLesson().getId(),
                "studentId", submission.getStudent().getId(),
                "reviewerId", reviewerId,
                "fromStatus", previousStatus,
                "toStatus", finalStatus,
                "completed", finalPassed,
                "points", totalAwardedPoints);

        return new SubmissionResultDto(
                submission.getId(),
                submission.getStatus()
        );
    }

    private static void validateReviewQuestionDecision(OpenReviewStatus reviewStatus, ReviewQuestionDecisionDto decision, Long questionId) {
        if (reviewStatus == OpenReviewStatus.PENDING_REVIEW) {
            throw new BadRequestException("PENDING_REVIEW is not allowed as review decision");
        }
        if ((OpenReviewStatus.REWORK == reviewStatus || OpenReviewStatus.REJECTED == reviewStatus)
                && decision.pointsType() != null) {
            throw new BadRequestException("pointsType should not be defined if reviewStatus not COMPLETED. Bad questionId: " + questionId);
        }
    }

    private PracticeLesson validateAndGetPracticeLesson(LessonSubmission submission, Long reviewerId) {
        if (!courseAssignmentService.canReviewCourse(submission.getLesson().getCourse().getId(), reviewerId)) {
            throw new AccessDeniedException("Admin is not assigned as reviewer for this course");
        }
        if (SubmissionStatus.FINAL_STATUSES.contains(submission.getStatus())) {
            throw new BadRequestException("Submission is already finalized");
        }
        if (submission.getStatus() != SubmissionStatus.PENDING_REVIEW) {
            throw new BadRequestException("Submission is not in reviewable status");
        }

        return lessonRepository.findPracticeLessonById(submission.getLesson().getId())
                .orElseThrow(() -> new BadRequestException("Submission is not OPEN_ANSWER practice lesson"));
    }

    private PracticeLesson validateSubmissionForReviewAndGetPracticeLesson(LessonSubmission submission, Long reviewerId) {
        if (SubmissionStatus.FINAL_STATUSES.contains(submission.getStatus())) {
            throw new BadRequestException("Submission is already finalized");
        }
        if (submission.getStatus() != SubmissionStatus.PENDING_REVIEW) {
            throw new BadRequestException("Submission is not in reviewable status");
        }

        if (!courseAssignmentService.canReviewCourse(submission.getLesson().getCourse().getId(), reviewerId)) {
            throw new AccessDeniedException("Admin is not assigned as reviewer for this course");
        }

        return lessonRepository.findPracticeLessonById(submission.getLesson().getId())
                .orElseThrow(() -> new BadRequestException("Submission is not OPEN_ANSWER practice lesson"));
    }
}
