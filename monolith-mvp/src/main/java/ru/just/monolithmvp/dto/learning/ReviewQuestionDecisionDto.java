package ru.just.monolithmvp.dto.learning;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ru.just.monolithmvp.model.OpenReviewStatus;
import ru.just.monolithmvp.model.QuestionPointsType;

@Schema(description = "Решение ревьювера по одному вопросу")
public record ReviewQuestionDecisionDto(
        @Schema(description = "Статус проверки вопроса", example = "ACCEPTED")
        @NotNull OpenReviewStatus submissionStatus,
        @Schema(description = "Тип баллов за вопрос. Должен быть null, если submissionStatus != ACCEPTED", example = "FULL")
        QuestionPointsType pointsType,
        @Schema(description = "Комментарий ревьювера", example = "Хорошо, пойдет")
        String reviewComment
) {
}
