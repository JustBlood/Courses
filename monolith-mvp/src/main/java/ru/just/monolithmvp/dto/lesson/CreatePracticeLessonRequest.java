package ru.just.monolithmvp.dto.lesson;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.just.monolithmvp.model.LessonType;
import ru.just.monolithmvp.model.QuestionType;

import java.util.List;

public record CreatePracticeLessonRequest(
        @NotNull Integer position,
        @NotBlank String title,
        @NotNull LessonType lessonType,
        QuestionType questionType,
        String questionText,
        String assignmentPrompt,
        List<String> options,
        List<String> correctAnswers,
        @Min(0) Integer fullPoints,
        @Min(0) Integer partialPoints
) {
}
