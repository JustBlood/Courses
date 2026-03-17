package ru.just.monolithmvp.dto.learning;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Ответ студента на практический урок")
public record PracticeSubmissionRequest(
        @Schema(description = "Ответы по id вопросов: questionId -> answers[]")
        Map<Long, List<String>> questionAnswers
) {
}
