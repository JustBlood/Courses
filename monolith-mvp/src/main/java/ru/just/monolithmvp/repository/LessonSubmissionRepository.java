package ru.just.monolithmvp.repository;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.just.monolithmvp.model.LessonSubmission;
import ru.just.monolithmvp.model.SubmissionStatus;
import ru.just.monolithmvp.repository.projection.UserCourseMetricProjection;

import java.util.List;
import java.util.Optional;

public interface LessonSubmissionRepository extends JpaRepository<LessonSubmission, Long> {
    List<LessonSubmission> findAllByStatusAndLessonCourseIdIn(SubmissionStatus status, List<Long> courseId);
    @Query("""
        select s
        from LessonSubmission s join fetch s.lesson l
        where s.student.id = :studentId and l.course.id = :courseId
        """)
    List<LessonSubmission> findByStudentIdAndLessonCourseId(Long studentId, Long courseId);

    @Query("""
        select s
        from LessonSubmission s join fetch s.lesson l
        where s.student.id in :userIds and l.course.id in :courseIds
        """)
    List<LessonSubmission> findByStudentIdInAndLessonCourseIdIn(@Param("userIds") List<Long> userIds,
                                                                 @Param("courseIds") List<Long> courseIds);
    Optional<LessonSubmission> findByStudentIdAndLessonId(Long studentId, Long lessonId);
    long countByStudentIdAndLessonId(Long studentId, Long lessonId);

    Optional<LessonSubmission> findFirstByStudentIdAndLessonIdAndStatusInOrderBySubmittedAtDesc(Long studentId,
                                                                                                Long lessonId,
                                                                                                List<SubmissionStatus> status);

    void deleteByStudentId(Long studentId);
    void deleteByLesson_Course_Id(Long courseId);
    void deleteByStudentIdAndLesson_Course_Id(Long studentId, Long courseId);
    void deleteByLessonId(Long lessonId);

    @Query("""
        select s
        from LessonSubmission s
        where s.student.id = :studentId
                and s.lesson.position < :position
                and s.lesson.course.id = :courseId
                and s.status in ('COMPLETED', 'PENDING_REVIEW')
        """)
    List<LessonSubmission> findAnsweredLessonsByCourse(Long studentId, Integer position, Long courseId);

    void deleteByStudentIdAndLessonId(Long userId, Long lessonId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from LessonSubmission s
        where s.lesson.id = :lessonId
        """)
    void deleteAllByLessonId(Long lessonId);

    List<LessonSubmission> findAllByLessonId(Long lessonId);

    @EntityGraph(attributePaths = {"lesson", "student"})
    List<LessonSubmission> findAllByStatusIn(List<SubmissionStatus> status);

    @Query("""
        select s.student.id as userId, s.lesson.course.id as courseId, count(distinct s.lesson.id) as value
        from LessonSubmission s
        where s.status = 'COMPLETED' and s.student.id in :userIds and s.lesson.course.id in :courseIds
        group by s.student.id, s.lesson.course.id
        """)
    List<UserCourseMetricProjection> countCompletedLessonsByUserIdsAndCourseIds(@Param("userIds") List<Long> userIds,
                                                                                 @Param("courseIds") List<Long> courseIds);

    @Query("""
            select s.student.id as userId,
                   s.lesson.course.id as courseId,
                   coalesce(sum(case when s.attemptCounter > 1 then s.attemptCounter - 1 else 0 end), 0) as value
            from LessonSubmission s
            where s.student.id in :userIds and s.lesson.course.id in :courseIds
            group by s.student.id, s.lesson.course.id
            """)
    List<UserCourseMetricProjection> sumRetakesByUserIdsAndCourseIds(@Param("userIds") List<Long> userIds,
                                                                      @Param("courseIds") List<Long> courseIds);
}
