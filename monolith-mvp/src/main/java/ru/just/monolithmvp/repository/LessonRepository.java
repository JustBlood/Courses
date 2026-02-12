package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.Lesson;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByCourseIdOrderByPositionAsc(Long courseId);
    boolean existsByCourseIdAndPosition(Long courseId, Integer position);
}
