package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.just.monolithmvp.model.CourseProgressStatus;
import ru.just.monolithmvp.model.Enrollment;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @Query("""
            select e
            from Enrollment e
            join fetch e.user
            join fetch e.course
            where e.user.id = :userId
            """)
    List<Enrollment> findByUserIdWithUserAndCourse(Long userId);

    List<Enrollment> findByUserId(Long userId);

    @Query("""
            select e
            from Enrollment e
            join fetch e.user
            join fetch e.course
            where e.course.id = :courseId
            """)
    List<Enrollment> findByCourseIdWithUserAndCourse(Long courseId);

    List<Enrollment> findByCourseId(Long courseId);

    List<Enrollment> findByUserIdAndCourseIdIn(Long userId, List<Long> courseIds);

    @Query("""
            select e
            from Enrollment e
            join fetch e.user
            join fetch e.course
            join CourseProgress cp on cp.user.id = e.user.id and cp.course.id = e.course.id and cp.status = :status
            """)
    List<Enrollment> findAllWithUserAndCourseByProgressStatus(CourseProgressStatus status);

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    void deleteByUserIdAndCourseId(Long userId, Long courseId);

    void deleteByUserId(Long userId);

    void deleteByCourseId(Long courseId);
}
