package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.model.Lesson;
import ru.just.monolithmvp.repository.LessonRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LessonAccessPolicy {
    private final CourseAccessPolicy courseAccessPolicy;
    private final LessonRepository lessonRepository;
    private final LessonSubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public void validateStudentEnrolled(Long userId, Long courseId) {
        courseAccessPolicy.assertStudentEnrolled(userId, courseId);
    }

    @Transactional(readOnly = true)
    public void assertLessonAccessAllowed(Long studentId, Lesson lesson) {
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

    @Transactional(readOnly = true)
    public void assertStopLessonAccessAllowed(Long studentId, Lesson lesson) {
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
}
