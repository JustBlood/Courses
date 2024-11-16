package ru.just.courses.repository.lesson;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.courses.model.theme.lesson.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
}
