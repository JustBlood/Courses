package ru.just.courses.repository.lesson;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.courses.model.theme.lesson.HtmlLesson;
import ru.just.courses.repository.projection.HtmlContentProjection;

import java.util.Optional;

public interface HtmlLessonRepository extends JpaRepository<HtmlLesson, Long> {
    Optional<HtmlContentProjection> findHtmlByLessonId(Long lessonId);
}
