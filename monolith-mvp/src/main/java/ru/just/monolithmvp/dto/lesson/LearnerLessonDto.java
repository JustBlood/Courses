package ru.just.monolithmvp.dto.lesson;

import lombok.Builder;
import ru.just.monolithmvp.model.LessonType;
import ru.just.monolithmvp.model.TheoryContentType;

import java.util.List;

@Builder
public record LearnerLessonDto (
    Long id,
    Integer position,
    String title,
    String description,
    LessonType lessonType,
    TheoryContentType theoryContentType,
    String theoryContent,
    List<LearnerPracticeQuestionDto> questions
) {

}
