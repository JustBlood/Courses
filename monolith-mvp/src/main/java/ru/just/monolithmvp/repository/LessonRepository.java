package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.Lesson;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByCourse_IdAndPositionGreaterThan(Long courseId, Integer deletingLessonPosition);
    List<Lesson> findByCourseIdOrderByPositionAsc(Long courseId);
    Lesson findByCourse_IdOrderByPositionDesc(Long courseId);
    boolean existsByCourseIdAndPosition(Long courseId, Integer position);
    boolean existsByCourseIdAndPositionAndIdNot(Long courseId, Integer position, Long lessonId);
    long countByCourseId(Long courseId);
}
