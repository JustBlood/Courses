package ru.just.courses.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.just.courses.dto.lesson.LessonDto;
import ru.just.courses.mapper.converter.LessonConverter;
import ru.just.courses.model.theme.lesson.Lesson;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class LessonMapper implements Function<Lesson, LessonDto> {

    private final List<LessonConverter> lessonConverters;

    @Override
    public LessonDto apply(Lesson lesson) {
        return lessonConverters.stream()
                .filter(converter -> converter.canHandle(lesson.getType()))
                .map(converter -> (LessonDto) converter.apply(lesson))
                .findAny().orElseThrow(() -> new IllegalStateException("Не найден конвертер дто для типа урока: " + lesson.getType()));
    }
}
