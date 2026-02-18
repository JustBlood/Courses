package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.learning.PendingSubmissionDto;
import ru.just.monolithmvp.dto.learning.PracticeSubmissionRequest;
import ru.just.monolithmvp.dto.learning.ReviewOpenSubmissionRequest;
import ru.just.monolithmvp.dto.learning.SubmissionResultDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.*;
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

        if (request.selectedAnswers() == null || request.selectedAnswers().isEmpty()) {
            throw new BadRequestException("selectedAnswers is required for choice tasks");
        }

        PracticeQuestion firstQuestion = practiceLesson.getQuestions().stream()
                .min(Comparator.comparing(PracticeQuestion::getQuestionIndex))
                .orElseThrow(() -> new BadRequestException("Practice lesson has no questions"));

        List<String> selectedAnswers = normalizeList(request.selectedAnswers());
        List<String> correctAnswers = normalizeList(splitRaw(firstQuestion.getCorrectAnswersRaw()));

        boolean correct = evaluateCorrectness(firstQuestion.getQuestionType(), selectedAnswers, correctAnswers);
        int points = 0;
        if (correct) {
            points = lesson.getFullPoints();
        } else if (firstQuestion.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            long matched = selectedAnswers.stream().filter(correctAnswers::contains).count();
            if (matched > 0) {
                points = lesson.getPartialPoints();
            }
        }
        submission.setAnswerRaw(String.join(";;", selectedAnswers));
        submission.setStatus(correct ? SubmissionStatus.COMPLETE : SubmissionStatus.INCOMPLETE);
        submission.setPassed(correct);
        submission.setPointsAwarded(points);
        submission = submissionRepository.save(submission);
        if (correct) {
            markEnrollmentCompletedIfDone(studentId, lesson.getCourse().getId());
        }

        return new SubmissionResultDto(
                submission.getId(),
                submission.getStatus(),
                submission.getPassed(),
                correct ? "Correct answer" : "Incorrect answer"
        );
    }

    @Transactional(readOnly = true)
    public List<PendingSubmissionDto> getPendingReviews() {
        Long adminId = securityUtils.currentUserId();
        return submissionRepository.findByStatus(SubmissionStatus.PENDING_REVIEW).stream()
                .filter(s -> courseService.canReviewCourse(s.getLesson().getCourse().getId(), adminId))
                .map(s -> new PendingSubmissionDto(
                        s.getId(),
                        s.getLesson().getId(),
                        s.getLesson().getTitle(),
                        s.getStudent().getId(),
                        s.getStudent().getEmail(),
                        s.getAnswerRaw(),
                        s.getSubmittedAt().toString()
                ))
                .toList();
    }

    @Transactional
    public SubmissionResultDto reviewOpenSubmission(Long submissionId, ReviewOpenSubmissionRequest request) {
        LessonSubmission submission = submissionRepository.findWithLockingById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found: " + submissionId));

        if (submission.getStatus() != SubmissionStatus.PENDING_REVIEW) {
            throw new BadRequestException("Submission is not pending review");
        }

        if (!courseService.canReviewCourse(submission.getLesson().getCourse().getId(), securityUtils.currentUserId())) {
            throw new BadRequestException("Admin is not assigned as reviewer for this course");
        }

        submission.setPassed(request.passed());
        submission.setStatus(request.passed() ? SubmissionStatus.COMPLETE : SubmissionStatus.INCOMPLETE);
        submission.setPointsAwarded(request.passed() ? submission.getLesson().getFullPoints() : 0);
        submission.setReviewComment(request.comment());
        submission.setReviewedByAdminId(securityUtils.currentUserId());
        submission.setReviewedAt(LocalDateTime.now());

        submission = submissionRepository.save(submission);
        if (request.passed()) {
            markEnrollmentCompletedIfDone(submission.getStudent().getId(), submission.getLesson().getCourse().getId());
        }
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
        if (user.getRole() != Role.STUDENT) {
            throw new BadRequestException("Only STUDENT can submit lessons");
        }
        return user;
    }

    private boolean evaluateCorrectness(QuestionType type, List<String> selected, List<String> correct) {
        if (type == QuestionType.ORDERING) {
            return Objects.equals(selected, correct);
        }
        return new HashSet<>(selected).equals(new HashSet<>(correct));
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
}
