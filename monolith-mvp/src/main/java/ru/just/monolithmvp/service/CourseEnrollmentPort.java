package ru.just.monolithmvp.service;

public interface CourseEnrollmentPort {
    void enrollStudentToCourse(Long courseId, Long userId);
}
