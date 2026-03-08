package ru.just.monolithmvp.dto.learning;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.just.monolithmvp.model.OpenReviewStatus;

@Schema(description = "Информация по открытому вопросу для ревью")
public record PendingSubmissionQuestionDto(
        @Schema(description = "Индекс вопроса", example = "1")
        Integer questionIndex,
        @Schema(description = "Статус вопроса в review", example = "PENDING_REVIEW")
        OpenReviewStatus submissionStatus,
        @Schema(description = "Текст вопроса", example = "Текст вопроса")
        String questionText,
        @Schema(description = "Подсказка для тренера", example = "Подсказка для тренера")
        String trainerHint,
        @Schema(description = "Выставленные баллы", example = "0")
        Integer awardedPoints,
        @Schema(description = "Максимальные баллы за вопрос", example = "20")
        Integer fullPoints,
        @Schema(description = "Ответ студента", example = "Ответ, данный учеником")
        String answer,
        @Schema(description = "Комментарий ревьювера", example = "Не до конца понял твою мысль")
        String reviewComment
) {
}