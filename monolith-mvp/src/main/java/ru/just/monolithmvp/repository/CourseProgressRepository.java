package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.CourseProgress;
import ru.just.monolithmvp.model.CourseProgressStatus;

import java.util.List;
import java.util.Optional;

public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {
    Optional<CourseProgress> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    void deleteByUserIdAndCourseId(Long userId, Long courseId);

    List<CourseProgress> findByUserIdAndCourseIdIn(Long userId, List<Long> courseIds);

    List<CourseProgress> findByUserIdInAndCourseIdIn(List<Long> userIds, List<Long> courseIds);

    List<CourseProgress> findAllByCourseId(Long courseId);

    void deleteByUserIdAndCourseIdAndStatus(Long userId, Long courseId, CourseProgressStatus courseProgressStatus);
}
