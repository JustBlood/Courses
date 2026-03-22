package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.PracticeQuestion;

import java.util.List;

public interface PracticeQuestionRepository extends JpaRepository<PracticeQuestion, Long> {
    List<PracticeQuestion> findByLessonIdOrderByQuestionIndexAsc(Long lessonId);
    List<PracticeQuestion> findByLessonIdIn(List<Long> lessonIds);
    long countByLessonId(Long lessonId);
    void deleteAllByLessonId(Long lessonId);
}
