package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.just.monolithmvp.model.Lesson;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByCourse_IdAndPositionGreaterThan(Long courseId, Integer deletingLessonPosition);
    List<Lesson> findByCourse_IdAndPositionGreaterThanEqualOrderByPositionDesc(Long courseId, Integer position);
    List<Lesson> findByCourse_IdAndPositionBetweenOrderByPositionAsc(Long courseId, Integer fromInclusive, Integer toInclusive);
    List<Lesson> findByCourseIdOrderByPositionAsc(Long courseId);
    Lesson findFirstByCourse_IdOrderByPositionDesc(Long courseId);
    Optional<Lesson> findFirstByCourse_IdAndPositionLessThanOrderByPositionDesc(Long courseId, Integer position);

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
                      and s.completed = true
              )
            """)
    boolean existsUncompletedStopLessonBeforePosition(Long courseId, Long studentId, Integer targetPosition);
}
