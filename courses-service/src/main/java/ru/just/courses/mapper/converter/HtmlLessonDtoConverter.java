package ru.just.courses.mapper.converter;

import org.springframework.stereotype.Component;
import ru.just.courses.dto.lesson.LessonDto;
import ru.just.courses.model.theme.lesson.HtmlLesson;
import ru.just.courses.model.theme.lesson.LessonType;

@Component
public class HtmlLessonDtoConverter implements LessonConverter<HtmlLesson, LessonDto> {
    @Override
    public LessonDto apply(HtmlLesson lesson) {
        return LessonDto.HtmlLessonDto.builder()
                .lessonId(lesson.getLessonId())
                .type(lesson.getType())
                .ordinalNumber(lesson.getOrdinalNumber())
                .build();
    }

    @Override
    public boolean canHandle(LessonType lessonType) {
        return LessonType.HTML.equals(lessonType);
    }
}
