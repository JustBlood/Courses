package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.LessonSubmissionStatusHistory;

import java.util.List;

public interface LessonSubmissionStatusHistoryRepository extends JpaRepository<LessonSubmissionStatusHistory, Long> {
    List<LessonSubmissionStatusHistory> findAllBySubmissionIdOrderByIdAsc(Long submissionId);
}
