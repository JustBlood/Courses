package ru.just.monolithmvp.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.just.monolithmvp.model.LessonSubmission;
import ru.just.monolithmvp.model.SubmissionStatus;

import java.util.List;
import java.util.Optional;

public interface LessonSubmissionRepository extends JpaRepository<LessonSubmission, Long> {
    List<LessonSubmission> findAllByStatusAndLessonCourseIdIn(SubmissionStatus status, List<Long> courseId);
    List<LessonSubmission> findByStudentIdAndLessonCourseId(Long studentId, Long courseId);

    Optional<LessonSubmission> findFirstByStudentIdAndLessonIdAndPassedTrueOrderBySubmittedAtDesc(Long studentId,
                                                                                                    Long lessonId);
    Optional<LessonSubmission> findFirstByStudentIdAndLessonIdAndStatusOrderBySubmittedAtDesc(Long studentId,
                                                                                                Long lessonId,
                                                                                                SubmissionStatus status);
    long countByStudentIdAndLessonId(Long studentId, Long lessonId);
    Optional<LessonSubmission> findFirstByStudentIdAndLessonIdOrderBySubmittedAtAsc(Long studentId, Long lessonId);

    @Query("select count(distinct s.lesson.id) from LessonSubmission s where s.student.id = :studentId and s.lesson.course.id = :courseId and s.passed = true")
    long countDistinctPassedLessons(Long studentId, Long courseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LessonSubmission> findWithLockingById(Long id);

    void deleteByStudentId(Long studentId);
    void deleteByLesson_Course_Id(Long courseId);
    void deleteByStudentIdAndLesson_Course_Id(Long studentId, Long courseId);
    void deleteByLessonId(Long lessonId);
}
