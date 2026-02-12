package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.LessonSubmission;
import ru.just.monolithmvp.model.SubmissionStatus;

import java.util.List;

public interface LessonSubmissionRepository extends JpaRepository<LessonSubmission, Long> {
    List<LessonSubmission> findByStatus(SubmissionStatus status);
    List<LessonSubmission> findByStudentIdAndLessonCourseId(Long studentId, Long courseId);
    boolean existsByStudentIdAndLessonIdAndPassedTrue(Long studentId, Long lessonId);
    void deleteByStudentId(Long studentId);
}
