package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EnrollmentProgressService {
    private final EnrollmentRepository enrollmentRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final CourseLessonAdminService courseLessonAdminService;
    private final ProgramService programService;

    @Transactional
    public void markEnrollmentStarted(Long userId, Long courseId) {
        enrollmentRepository.findByUserIdAndCourseId(userId, courseId).ifPresent(e -> {
            if (e.getStartedAt() == null) {
                e.setStartedAt(LocalDateTime.now());
                enrollmentRepository.save(e);
                programService.onCourseProgressChanged(userId, courseId);
            }
        });
    }

    @Transactional
    public void markEnrollmentCompletedIfDone(Long userId, Long courseId) {
        long passedLessons = submissionRepository.countDistinctCompletedLessons(userId, courseId);
        long totalLessons = courseLessonAdminService.getCourseLessons(courseId).size();
        if (totalLessons > 0 && passedLessons >= totalLessons) {
            enrollmentRepository.findByUserIdAndCourseId(userId, courseId).ifPresent(e -> {
                if (e.getCompletedAt() == null) {
                    e.setCompletedAt(LocalDateTime.now());
                    enrollmentRepository.save(e);
                    programService.onCourseProgressChanged(userId, courseId);
                }
            });
        }
    }
}
