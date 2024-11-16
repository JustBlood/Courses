package ru.just.courses.mapper.converter;

import org.springframework.stereotype.Component;
import ru.just.courses.dto.lesson.LessonDto;
import ru.just.courses.model.theme.lesson.LessonType;
import ru.just.courses.model.theme.lesson.MultiTestLesson;

@Component
public class MultiTestLessonDtoConverter implements LessonConverter<MultiTestLesson, LessonDto> {
    @Override
    public LessonDto apply(MultiTestLesson lesson) {
        return LessonDto.MultiTestLessonDto.builder()
                .lessonId(lesson.getLessonId())
                .condition(lesson.getCondition())
                .possibleAnswers(lesson.getPossibleAnswers())
                .correctAnswers(lesson.getCorrectAnswers())
                .type(lesson.getType())
                .ordinalNumber(lesson.getOrdinalNumber())
                .build();
    }

    @Override
    public boolean canHandle(LessonType lessonType) {
        return LessonType.MULTI_TEST.equals(lessonType);
    }
}
