package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.*;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CourseEnrollmentLifecycleService {
    private final EnrollmentRepository enrollmentRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final LessonSubmissionRepository lessonSubmissionRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final ProgramCourseRepository programCourseRepository;

    @Transactional
    public void assignToCourse(Long userId, Long courseId) {
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            Enrollment enrollment = new Enrollment();
            Course course = new Course();
            course.setId(courseId);
            enrollment.setCourse(course);

            AppUser user = new AppUser();
            user.setId(userId);
            enrollment.setUser(user);
            enrollment.setEnrolledAt(LocalDateTime.now(Clock.systemUTC()));
            enrollmentRepository.save(enrollment);
        }

        if (!courseProgressRepository.existsByUserIdAndCourseId(userId, courseId)) {
            CourseProgress progress = new CourseProgress();
            Course course = new Course();
            course.setId(courseId);
            progress.setCourse(course);

            AppUser user = new AppUser();
            user.setId(userId);
            progress.setUser(user);

            progress.setStatus(CourseProgressStatus.NEW);
            courseProgressRepository.save(progress);
        }
    }

    @Transactional
    public void unassignFromCourse(Long userId, Long courseId, boolean deleteProgress) {
        enrollmentRepository.deleteByUserIdAndCourseId(userId, courseId);
        if (deleteProgress) {
            courseProgressRepository.deleteByUserIdAndCourseId(userId, courseId);
            lessonSubmissionRepository.deleteByStudentIdAndLesson_Course_Id(userId, courseId);
        } else {
            courseProgressRepository.deleteByUserIdAndCourseIdAndStatus(userId, courseId, CourseProgressStatus.NEW);
        }
    }

    @Transactional
    public void unassignFromProgram(Long programId, Long userId) {
        programEnrollmentRepository.findByUserIdAndProgramId(userId, programId)
                .ifPresent(programEnrollmentRepository::delete);

        programCourseRepository.findByProgramIdOrderByOrderIndexAsc(programId)
                .forEach(pc -> unassignFromCourse(userId, pc.getCourse().getId(), false));
    }

    @Transactional
    public void resetCourseProgress(Long userId, Long courseId) {
        courseProgressRepository.findByUserIdAndCourseId(userId, courseId)
                .ifPresent(progress -> {
                    progress.setStartedAt(null);
                    progress.setCompletedAt(null);
                    progress.setStatus(CourseProgressStatus.NEW);
                    courseProgressRepository.save(progress);
                });
        lessonSubmissionRepository.deleteByStudentIdAndLesson_Course_Id(userId, courseId);
    }
}
