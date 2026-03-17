package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.Lesson;
import ru.just.monolithmvp.model.LessonSubmission;
import ru.just.monolithmvp.repository.LessonRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonAccessPolicy {
    private final LessonRepository lessonRepository;
    private final LessonSubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public void assertLessonAccessAllowed(Long studentId, Long lessonId) {
        final Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new NotFoundException("Lesson not found"));
        if (lesson.getPosition() <= 1 || Boolean.TRUE.equals(lesson.getCourse().getLessonsFreeOrder())) {
            return;
        }

        final List<LessonSubmission> alreadyCompletedLessons = submissionRepository.findAnsweredLessonsByCourse(studentId, lesson.getPosition(), lesson.getCourse().getId());

        if (alreadyCompletedLessons.size() == lesson.getPosition() - 1) {
            return;
        }

        throw new BadRequestException("Previous lessons is not completed");
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
            throw new BadRequestException("Previous stop lesson is not completed");
        }
    }
}
