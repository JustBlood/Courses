package ru.just.monolithmvp.dto.lesson;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.just.monolithmvp.model.QuestionType;

import java.util.List;

public record CreatePracticeLessonRequest(
        @NotNull Integer position,
        @NotBlank String title,
        @NotNull QuestionType questionType,
        @NotBlank String questionText,
        List<String> options,
        List<String> correctAnswers
) {
}
