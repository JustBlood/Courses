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
import ru.just.monolithmvp.model.AppUser;
import ru.just.monolithmvp.model.Lesson;
import ru.just.monolithmvp.model.LessonSubmission;
import ru.just.monolithmvp.model.LessonType;
import ru.just.monolithmvp.model.PracticeLesson;
import ru.just.monolithmvp.model.QuestionType;
import ru.just.monolithmvp.model.Role;
import ru.just.monolithmvp.model.SubmissionStatus;
import ru.just.monolithmvp.repository.AppUserRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LearningService {
    private final CourseService courseService;
    private final LessonSubmissionRepository submissionRepository;
    private final AppUserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public SubmissionResultDto completeTheoryLesson(Long lessonId) {
        Long studentId = securityUtils.currentUserId();
        Lesson lesson = courseService.getLessonEntity(lessonId);
        validateStudentEnrolled(studentId, lesson.getCourse().getId());

        if (lesson.getLessonType() != LessonType.THEORY) {
            throw new BadRequestException("Lesson is not THEORY");
        }

        LessonSubmission submission = new LessonSubmission();
        submission.setLesson(lesson);
        submission.setStudent(getStudent(studentId));
        submission.setStatus(SubmissionStatus.CORRECT);
        submission.setPassed(true);
        submission.setSubmittedAt(LocalDateTime.now());

        submission = submissionRepository.save(submission);
        return new SubmissionResultDto(submission.getId(), submission.getStatus(), true, "Theory lesson completed");
    }

    @Transactional
    public SubmissionResultDto submitPractice(Long lessonId, PracticeSubmissionRequest request) {
        Long studentId = securityUtils.currentUserId();
        Lesson lesson = courseService.getLessonEntity(lessonId);
        validateStudentEnrolled(studentId, lesson.getCourse().getId());

        if (lesson.getLessonType() != LessonType.PRACTICE) {
            throw new BadRequestException("Lesson is not PRACTICE");
        }

        LessonSubmission submission = new LessonSubmission();
        submission.setLesson(lesson);
        submission.setStudent(getStudent(studentId));
        submission.setSubmittedAt(LocalDateTime.now());

        if (!(lesson instanceof PracticeLesson practiceLesson)) {
            throw new BadRequestException("Lesson is not PRACTICE");
        }

        if (practiceLesson.getQuestionType() == QuestionType.OPEN_TEXT) {
            if (request.openAnswer() == null || request.openAnswer().isBlank()) {
                throw new BadRequestException("openAnswer is required for OPEN_TEXT task");
            }
            submission.setAnswerRaw(request.openAnswer());
            submission.setStatus(SubmissionStatus.PENDING_REVIEW);
            submission.setPassed(false);
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

        List<String> selectedAnswers = normalizeList(request.selectedAnswers());
        List<String> correctAnswers = normalizeList(splitRaw(practiceLesson.getCorrectAnswersRaw()));

        boolean correct = new HashSet<>(selectedAnswers).equals(new HashSet<>(correctAnswers));
        submission.setAnswerRaw(String.join(";;", selectedAnswers));
        submission.setStatus(correct ? SubmissionStatus.CORRECT : SubmissionStatus.INCORRECT);
        submission.setPassed(correct);
        submission = submissionRepository.save(submission);

        return new SubmissionResultDto(
                submission.getId(),
                submission.getStatus(),
                submission.getPassed(),
                correct ? "Correct answer" : "Incorrect answer"
        );
    }

    @Transactional(readOnly = true)
    public List<PendingSubmissionDto> getPendingReviews() {
        return submissionRepository.findByStatus(SubmissionStatus.PENDING_REVIEW).stream()
                .map(s -> new PendingSubmissionDto(
                        s.getId(),
                        s.getLesson().getId(),
                        s.getLesson().getTitle(),
                        s.getStudent().getId(),
                        s.getStudent().getUsername(),
                        s.getAnswerRaw(),
                        s.getSubmittedAt().toString()
                ))
                .toList();
    }

    @Transactional
    public SubmissionResultDto reviewOpenSubmission(Long submissionId, ReviewOpenSubmissionRequest request) {
        LessonSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found: " + submissionId));

        if (submission.getStatus() != SubmissionStatus.PENDING_REVIEW) {
            throw new BadRequestException("Submission is not pending review");
        }

        submission.setPassed(request.passed());
        submission.setStatus(request.passed() ? SubmissionStatus.CORRECT : SubmissionStatus.INCORRECT);
        submission.setReviewComment(request.comment());
        submission.setReviewedByAdminId(securityUtils.currentUserId());
        submission.setReviewedAt(LocalDateTime.now());

        submission = submissionRepository.save(submission);
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
