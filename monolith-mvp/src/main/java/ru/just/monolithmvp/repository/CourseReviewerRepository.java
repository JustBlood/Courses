package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.CourseReviewer;

public interface CourseReviewerRepository extends JpaRepository<CourseReviewer, Long> {
    boolean existsByCourseIdAndReviewerId(Long courseId, Long reviewerId);
    void deleteByCourseIdAndReviewerId(Long courseId, Long reviewerId);
}
