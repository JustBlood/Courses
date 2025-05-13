package ru.just.courses.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import ru.just.courses.model.course.Course;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTitleLikeIgnoreCase(String title);

    List<Course> findByAuthorIdAndIsPublished(Long authorId, Boolean isPublished);

    @Modifying
    @Transactional
    @Query("UPDATE Course e SET e.isPublished = :isPublished WHERE e.id = :id")
    void updateIsPublished(Long id, Boolean isPublished);

    List<Course> findByAuthorId(Long authorId);

    @Query("FROM Course c JOIN FETCH c.modules m JOIN FETCH m.themes th JOIN FETCH th.lessons where c.id = :courseId")
    Optional<Course> findByIdFull(Long courseId);
}
