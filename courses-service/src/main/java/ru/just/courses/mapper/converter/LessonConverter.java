package ru.just.courses.mapper.converter;

import ru.just.courses.dto.lesson.LessonDto;
import ru.just.courses.model.theme.lesson.Lesson;
import ru.just.courses.model.theme.lesson.LessonType;

import java.util.function.Function;

public interface LessonConverter<E extends Lesson, DTO extends LessonDto> extends Function<E, DTO> {
    boolean canHandle(LessonType lessonType);
}
