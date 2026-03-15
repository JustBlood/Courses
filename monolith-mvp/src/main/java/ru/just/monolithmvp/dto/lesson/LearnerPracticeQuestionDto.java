package ru.just.monolithmvp.dto.lesson;

import ru.just.monolithmvp.model.OpenReviewStatus;
import ru.just.monolithmvp.model.QuestionType;

import java.util.List;

public record LearnerPracticeQuestionDto(
        Integer position,
        QuestionType questionType,
        String questionText,
        List<String> options,
        List<String> userAnswers,
        List<String> correctAnswers,
        OpenReviewStatus status,
        String reviewComment,
        Integer awardedPoints,
        Integer fullPoints,
        Integer partialPoints
) {
}
