package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.just.monolithmvp.model.Lesson;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByCourse_IdAndPositionGreaterThan(Long courseId, Integer deletingLessonPosition);
    List<Lesson> findByCourseIdOrderByPositionAsc(Long courseId);
    Lesson findFirstByCourse_IdOrderByPositionDesc(Long courseId);
    boolean existsByCourseIdAndPosition(Long courseId, Integer position);
    boolean existsByCourseIdAndPositionAndIdNot(Long courseId, Integer position, Long lessonId);
    long countByCourseId(Long courseId);

    @Query("""
            select count(l) > 0
            from Lesson l
            where l.course.id = :courseId
              and l.position < :targetPosition
              and l.stopLesson = true
              and not exists (
                    select s.id
                    from LessonSubmission s
                    where s.lesson.id = l.id
                      and s.student.id = :studentId
                      and s.passed = true
              )
            """)
    boolean existsUnpassedStopLessonBeforePosition(Long courseId, Long studentId, Integer targetPosition);
}
