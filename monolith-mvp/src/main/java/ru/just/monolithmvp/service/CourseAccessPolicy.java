package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.model.Enrollment;
import ru.just.monolithmvp.repository.EnrollmentRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CourseAccessPolicy {
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public boolean isUserEnrolled(Long userId, Long courseId) {
        return enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    @Transactional(readOnly = true)
    public void assertStudentEnrolled(Long userId, Long courseId) {
        if (!isUserEnrolled(userId, courseId)) {
            throw new BadRequestException("Student is not enrolled in this course");
        }
    }

    @Transactional(readOnly = true)
    public void assertCourseDeadlineNotExceededForStudent(Long userId, Long courseId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new BadRequestException("Student is not enrolled on this course"));
        Integer deadlineDays = enrollment.getCourse().getDeadlineDays();
        if (deadlineDays == null) {
            return;
        }

        LocalDateTime deadlineAt = enrollment.getEnrolledAt().plusDays(deadlineDays);
        if (LocalDateTime.now().isAfter(deadlineAt)) {
            throw new BadRequestException("Course deadline exceeded");
        }
    }
}
