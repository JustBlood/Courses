package ru.just.monolithmvp.dto.lesson;

import ru.just.monolithmvp.model.LessonType;
import ru.just.monolithmvp.model.QuestionType;
import ru.just.monolithmvp.model.TheoryContentType;

import java.util.List;

public record LessonDto(
        Long id,
        Long courseId,
        Integer position,
        String title,
        LessonType lessonType,
        TheoryContentType theoryContentType,
        String theoryContent,
        QuestionType questionType,
        String questionText,
        String assignmentPrompt,
        List<String> options,
        Integer fullPoints,
        Integer partialPoints
) {
}
