package ru.just.monolithmvp.dto.lesson;

import lombok.Builder;
import ru.just.monolithmvp.model.LessonType;

import java.util.List;

@Builder
public record LearnerLessonDto (
    Long id,
    Integer position,
    String title,
    String description,
    LessonType lessonType,
    String theoryContent,
    List<LearnerPracticeQuestionDto> questions
) {

}
