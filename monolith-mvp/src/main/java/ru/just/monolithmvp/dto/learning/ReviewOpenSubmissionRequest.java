package ru.just.monolithmvp.dto.learning;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Результат ручной проверки open-ended ответа")
public record ReviewOpenSubmissionRequest(
        @Schema(description = "Финальный результат проверки", example = "true")
        boolean passed,
        @Schema(description = "Выдать частичные баллы", example = "false")
        boolean partialPoints,
        @Schema(description = "Вернуть на доработку", example = "true")
        boolean toNextReview,
        @Schema(description = "Комментарий проверяющего", example = "Добавьте больше деталей в ответ")
        String comment
) {
}
