package ru.just.monolithmvp.dto.lesson;

import ru.just.monolithmvp.model.QuestionType;

import java.util.List;

public record PracticeQuestionDto(
        Long id,
        Integer position,
        QuestionType questionType,
        String questionText,
        String trainerHint,
        List<String> options,
        List<String> correctAnswers,
        Integer fullPoints,
        Integer partialPoints
) {
}