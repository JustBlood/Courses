package ru.just.monolithmvp.dto.lesson;

import jakarta.validation.constraints.NotBlank;
import ru.just.monolithmvp.model.QuestionType;

import java.util.List;

public record PracticeQuestionRequest(
        QuestionType questionType,
        @NotBlank String questionText,
        List<String> options,
        List<String> correctAnswers
) {
}