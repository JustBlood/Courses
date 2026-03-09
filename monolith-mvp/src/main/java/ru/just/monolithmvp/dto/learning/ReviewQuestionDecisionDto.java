package ru.just.monolithmvp.dto.learning;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.just.monolithmvp.model.OpenReviewStatus;

@Schema(description = "Решение ревьювера по одному вопросу")
public record ReviewQuestionDecisionDto(
        @Schema(description = "Статус проверки вопроса", example = "ACCEPTED")
        OpenReviewStatus submissionStatus,
        @Schema(description = "Баллы за вопрос", example = "10")
        Integer awardedPoints,
        @Schema(description = "Комментарий ревьювера", example = "Хорошо, пойдет")
        String reviewComment
) {
}