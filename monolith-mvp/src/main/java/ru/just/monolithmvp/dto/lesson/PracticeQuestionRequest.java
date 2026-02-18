package ru.just.monolithmvp.dto.lesson;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import ru.just.monolithmvp.model.QuestionType;

import java.util.List;

public record PracticeQuestionRequest(
        Integer position,
        QuestionType questionType,
        @NotBlank String questionText,
        String trainerHint,
        List<String> options,
        List<String> correctAnswers,
        @Min(0) Integer fullPoints,
        @Min(0) Integer partialPoints
) {
}
