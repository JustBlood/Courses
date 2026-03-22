package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.just.monolithmvp.model.Lesson;
import ru.just.monolithmvp.model.PracticeLesson;
import ru.just.monolithmvp.repository.projection.CourseMetricProjection;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByCourse_IdAndPositionGreaterThan(Long courseId, Integer deletingLessonPosition);
    List<Lesson> findByCourse_IdAndPositionBetweenOrderByPositionAsc(Long courseId, Integer fromInclusive, Integer toInclusive);
    List<Lesson> findByCourseIdOrderByPositionAsc(Long courseId);
    Lesson findFirstByCourse_IdOrderByPositionDesc(Long courseId);
    @Query("select l from PracticeLesson l where l.id = ?1")
    Optional<PracticeLesson> findPracticeLessonById(Long lessonId);

    long countByCourseId(Long courseId);

    @Query("""
            select l.course.id as courseId, coalesce(sum(l.fullPoints), 0L) as value
            from Lesson l
            where l.course.id in :courseIds
            group by l.course.id
            """)
    List<CourseMetricProjection> sumFullPointsByCourseIds(@Param("courseIds") List<Long> courseIds);

    @Query("""
            select l.course.id as courseId, count(l.id) as value
            from Lesson l
            where l.course.id in :courseIds
            group by l.course.id
            """)
    List<CourseMetricProjection> countLessonsByCourseIds(List<Long> courseIds);

    @Query("""
            select count(l) > 0
            from Lesson l join LessonSubmission s on  l.id = s.lesson.id and s.student.id = :studentId
            where l.course.id = :courseId
              and l.position < :targetPosition
              and l.stopLesson = true
              and s.status != 'COMPLETED'
            """)
    boolean existsUncompletedStopLessonBeforePosition(Long courseId, Long studentId, Integer targetPosition);
}
