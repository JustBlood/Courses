package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.model.AppUser;
import ru.just.monolithmvp.model.Course;
import ru.just.monolithmvp.model.CourseProgress;
import ru.just.monolithmvp.model.CourseProgressStatus;
import ru.just.monolithmvp.model.Enrollment;
import ru.just.monolithmvp.repository.CourseProgressRepository;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.repository.ProgramCourseRepository;
import ru.just.monolithmvp.repository.ProgramEnrollmentRepository;

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
            enrollment.setEnrolledAt(LocalDateTime.now());
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
    public void unassignFromCourse(Long userId, Long courseId) {
        CourseProgress progress = courseProgressRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);

        enrollmentRepository.deleteByUserIdAndCourseId(userId, courseId);

        boolean completed = progress != null && progress.getStatus() == CourseProgressStatus.COMPLETED;
        if (!completed) {
            courseProgressRepository.deleteByUserIdAndCourseId(userId, courseId);
            lessonSubmissionRepository.deleteByStudentIdAndLesson_Course_Id(userId, courseId);
        }
    }

    @Transactional
    public void unassignFromProgram(Long programId, Long userId) {
        programEnrollmentRepository.findByUserIdAndProgramId(userId, programId)
                .ifPresent(programEnrollmentRepository::delete);

        programCourseRepository.findByProgramIdOrderByOrderIndexAsc(programId)
                .forEach(pc -> unassignFromCourse(userId, pc.getCourse().getId()));
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
