package ru.just.monolithmvp.dto.lesson;

import ru.just.monolithmvp.model.QuestionType;

import java.util.List;

public record PracticeQuestionDto(
        Long id,
        Integer index,
        QuestionType questionType,
        String questionText,
        List<String> options
) {
}