package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.lesson.LessonDto;
import ru.just.monolithmvp.model.AppUser;
import ru.just.monolithmvp.model.Course;
import ru.just.monolithmvp.model.CourseProgress;
import ru.just.monolithmvp.model.CourseProgressStatus;
import ru.just.monolithmvp.repository.CourseProgressRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentProgressService {
    private final CourseProgressRepository courseProgressRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final CourseLessonAdminService courseLessonAdminService;
    private final ProgramService programService;

    @Transactional
    public void markEnrollmentStarted(Long userId, Long courseId) {
        CourseProgress progress = resolveOrCreateProgress(userId, courseId);
        if (progress.getStartedAt() == null) {
            progress.setStartedAt(LocalDateTime.now());
            if (progress.getStatus() != CourseProgressStatus.COMPLETED) {
                progress.setStatus(CourseProgressStatus.IN_PROGRESS);
            }
            courseProgressRepository.save(progress);
            programService.onCourseProgressChanged(userId, courseId);
        }
    }

    @Transactional
    public void markEnrollmentCompletedIfDone(Long userId, Long courseId) {
        long passedLessons = submissionRepository.countDistinctCompletedLessons(userId, courseId);
        final List<LessonDto> courseLessons = courseLessonAdminService.getCourseLessons(courseId);
        long totalLessons = courseLessons.size();
        int totalPoints = courseLessons.stream().map(LessonDto::fullPoints).reduce(Integer::sum).orElseThrow();
        if (totalLessons > 0 && passedLessons >= totalLessons) {
            CourseProgress progress = resolveOrCreateProgress(userId, courseId);
            if (progress.getCompletedAt() != null) {
                progress.setCompletedAt(LocalDateTime.now());
                progress.setStatus(CourseProgressStatus.COMPLETED);
                courseProgressRepository.save(progress);
                programService.onCourseProgressChanged(userId, courseId);
            }
        }
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
