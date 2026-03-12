package ru.just.monolithmvp.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.just.monolithmvp.model.LessonSubmission;
import ru.just.monolithmvp.model.SubmissionStatus;
import ru.just.monolithmvp.repository.projection.UserCourseMetricProjection;

import java.util.List;
import java.util.Optional;

public interface LessonSubmissionRepository extends JpaRepository<LessonSubmission, Long> {
    List<LessonSubmission> findAllByStatusAndLessonCourseIdIn(SubmissionStatus status, List<Long> courseId);
    List<LessonSubmission> findByStudentIdAndLessonCourseId(Long studentId, Long courseId);
    Optional<LessonSubmission> findByStudentIdAndLessonId(Long studentId, Long lessonId);
    long countByStudentIdAndLessonId(Long studentId, Long lessonId);

    Optional<LessonSubmission> findFirstByStudentIdAndLessonIdAndCompletedTrueOrderBySubmittedAtDesc(Long studentId,
                                                                                                    Long lessonId);

    @Query("""
            select s.student.id as userId, s.lesson.course.id as courseId, coalesce(sum(s.pointsAwarded), 0) as value
            from LessonSubmission s
            where s.student.id in :userIds and s.lesson.course.id in :courseIds
            group by s.student.id, s.lesson.course.id
            """)
    List<UserCourseMetricProjection> sumPointsByUserIdsAndCourseIds(List<Long> userIds, List<Long> courseIds);

    @Query("""
            select s.student.id as userId, s.lesson.course.id as courseId, count(distinct s.lesson.id) as value
            from LessonSubmission s
            where s.completed = true and s.student.id in :userIds and s.lesson.course.id in :courseIds
            group by s.student.id, s.lesson.course.id
            """)
    List<UserCourseMetricProjection> countCompletedLessonsByUserIdsAndCourseIds(List<Long> userIds, List<Long> courseIds);

    @Query("""
            select s.student.id as userId,
                   s.lesson.course.id as courseId,
                   coalesce(sum(case when s.attemptCounter > 1 then s.attemptCounter - 1 else 0 end), 0) as value
            from LessonSubmission s
            where s.student.id in :userIds and s.lesson.course.id in :courseIds
            group by s.student.id, s.lesson.course.id
            """)
    List<UserCourseMetricProjection> sumRetakesByUserIdsAndCourseIds(List<Long> userIds, List<Long> courseIds);

    @Query("select count(distinct s.lesson.id) from LessonSubmission s where s.student.id = :studentId and s.lesson.course.id = :courseId and s.completed = true")
    long countDistinctCompletedLessons(Long studentId, Long courseId);

    Optional<LessonSubmission> findWithLockingByStudentIdAndLessonId(Long studentId, Long lessonId);

    void deleteByStudentId(Long studentId);
    void deleteByLesson_Course_Id(Long courseId);
    void deleteByStudentIdAndLesson_Course_Id(Long studentId, Long courseId);
    void deleteByLessonId(Long lessonId);
}
