package ru.just.monolithmvp.dto.lesson;

import ru.just.monolithmvp.model.QuestionType;

import java.util.List;

public record LearnerPracticeQuestionDto(
        Integer index,
        QuestionType questionType,
        String questionText,
        List<String> options,
        Integer fullPoints,
        Integer partialPoints
) {
}
