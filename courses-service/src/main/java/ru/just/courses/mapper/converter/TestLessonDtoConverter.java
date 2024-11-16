package ru.just.courses.mapper.converter;

import org.springframework.stereotype.Component;
import ru.just.courses.dto.lesson.LessonDto;
import ru.just.courses.model.theme.lesson.LessonType;
import ru.just.courses.model.theme.lesson.TestLesson;

@Component
public class TestLessonDtoConverter implements LessonConverter<TestLesson, LessonDto> {
    @Override
    public LessonDto apply(TestLesson lesson) {
        return LessonDto.TestLessonDto.builder()
                .lessonId(lesson.getLessonId())
                .condition(lesson.getCondition())
                .possibleAnswers(lesson.getPossibleAnswers())
                .answer(lesson.getAnswer())
                .type(lesson.getType())
                .ordinalNumber(lesson.getOrdinalNumber())
                .build();
    }

    @Override
    public boolean canHandle(LessonType lessonType) {
        return LessonType.TEST.equals(lessonType);
    }
}
