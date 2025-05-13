package ru.just.courses.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.just.courses.model.course.Course;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTitleLikeIgnoreCase(String title);

    @Query("FROM Course c JOIN FETCH c.modules m JOIN FETCH m.themes th JOIN FETCH th.lessons where c.id = :courseId")
    Optional<Course> findByIdFull(Long courseId);
}
