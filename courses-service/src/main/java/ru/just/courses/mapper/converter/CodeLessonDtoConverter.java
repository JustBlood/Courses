package ru.just.courses.mapper.converter;

import org.springframework.stereotype.Component;
import ru.just.courses.dto.lesson.LessonDto;
import ru.just.courses.model.theme.lesson.CodeLesson;
import ru.just.courses.model.theme.lesson.LessonType;

@Component
public class CodeLessonDtoConverter implements LessonConverter<CodeLesson, LessonDto> {
    @Override
    public LessonDto apply(CodeLesson lesson) {
        return LessonDto.CodeLessonDto.builder()
                .lessonId(lesson.getLessonId())
                .condition(lesson.getCondition())
                .codeTests(lesson.getCodeTests())
                .type(lesson.getType())
                .ordinalNumber(lesson.getOrdinalNumber())
                .build();
    }

    @Override
    public boolean canHandle(LessonType lessonType) {
        return LessonType.CODE.equals(lessonType);
    }
}
