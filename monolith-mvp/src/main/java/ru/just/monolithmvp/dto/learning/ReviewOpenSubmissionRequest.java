package ru.just.monolithmvp.dto.learning;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Решения ревьювера по всем вопросам open-урока")
public record ReviewOpenSubmissionRequest(
        @Schema(description = "Решения по id вопросов: questionId -> решение ревью")
        Map<Long, ReviewQuestionDecisionDto> questionReviews
) {
}
