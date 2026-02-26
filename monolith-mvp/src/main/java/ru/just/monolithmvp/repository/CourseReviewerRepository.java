package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.CourseReviewer;

import java.util.List;

public interface CourseReviewerRepository extends JpaRepository<CourseReviewer, Long> {
    List<CourseReviewer> findAllByReviewerId(Long reviewerId);
    boolean existsByCourseIdAndReviewerId(Long courseId, Long reviewerId);
    void deleteByCourseIdAndReviewerId(Long courseId, Long reviewerId);
    void deleteByCourseId(Long courseId);
    List<CourseReviewer> findAllByCourseId(Long courseId);
}
