package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseProgressService {
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final LessonRepository lessonRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final ProgramService programService;

    @Transactional
    public void markCourseProgressStarted(Long userId, Long courseId, LocalDateTime startedAt) {
        CourseProgress progress = resolveOrCreateProgress(userId, courseId);
        if (progress.getStartedAt() == null) {
            progress.setStartedAt(startedAt);
            progress.setStatus(CourseProgressStatus.IN_PROGRESS);
            courseProgressRepository.saveAndFlush(progress);
            programService.onCourseProgressChanged(userId, courseId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalcCourseProgressByUserInNewTransaction(Long userId, Long courseId) {
        recalcCourseProgressByUser(userId, courseId);
    }

    @Transactional
    public void recalcCourseProgressByUser(Long userId, Long courseId) {
        final List<LessonSubmission> submissions = submissionRepository.findByStudentIdAndLessonCourseId(userId, courseId);
        long completedLessons = submissions.stream().map(LessonSubmission::getStatus).filter(SubmissionStatus.COMPLETED::equals).count();
        long incompletedLessons = submissions.stream().map(LessonSubmission::getStatus).filter(SubmissionStatus.INCOMPLETED::equals).count();
        final List<Lesson> courseLessons = getCourseLessons(courseId);
        long totalLessons = courseLessons.size();
        CourseProgress progress = resolveOrCreateProgress(userId, courseId);
        // курс назначен, но не начат
        if (CourseProgressStatus.NEW == progress.getStatus()) {
            log.error("Курс назначен, но не начат и вызван recalcCourseProgressByUser.");
            return;
        }

        final Course course = courseRepository.findById(courseId).orElseThrow();
        final Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElse(null);

        if (enrollment == null) {
            log.warn("Пользователь {} не записан на курс {}, прогресс не будет изменен", userId, courseId);
            return;
        }

        if (totalLessons <= 0 || completedLessons >= totalLessons) {
            // курс пройден
            if (progress.getCompletedAt() == null) {
                progress.setCompletedAt(LocalDateTime.now(Clock.systemUTC()));
                progress.setStatus(CourseProgressStatus.COMPLETED);
            }
        } else if (incompletedLessons > 0 || course.getDeadlineDays() != null && LocalDateTime.now(Clock.systemUTC()).isAfter(enrollment.getEnrolledAt().plusDays(course.getDeadlineDays()))) {
            // курс содержит проваленные уроки или истек дедлайн
            progress.setCompletedAt(null);
            progress.setStatus(CourseProgressStatus.INCOMPLETED);
        } else {
            // курс проходится
            progress.setCompletedAt(null);
            progress.setStatus(CourseProgressStatus.IN_PROGRESS);
        }

        courseProgressRepository.saveAndFlush(progress);
        programService.onCourseProgressChanged(userId, courseId);
    }

    @Transactional
    public void recalcCourseProgress(Long courseId) {
        final List<CourseProgress> courseProgresses = courseProgressRepository.findAllByCourseId(courseId);
        courseProgresses.forEach(courseProgress ->
                recalcCourseProgressByUser(courseProgress.getUser().getId(), courseId));
    }

    @Transactional(readOnly = true)
    public List<Lesson> getCourseLessons(Long courseId) {
        return lessonRepository.findByCourseIdOrderByPositionAsc(courseId).stream()
                .toList();
    }

    private CourseProgress resolveOrCreateProgress(Long userId, Long courseId) {
        return courseProgressRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseGet(() -> {
                    CourseProgress progress = new CourseProgress();
                    AppUser user = new AppUser();
                    user.setId(userId);
                    progress.setUser(user);

                    Course course = new Course();
                    course.setId(courseId);
                    progress.setCourse(course);

                    progress.setStatus(CourseProgressStatus.NEW);
                    return progress;
                });
    }
}
