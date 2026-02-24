package ru.just.monolithmvp.dto.learning;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Ответ студента на практический урок")
public record PracticeSubmissionRequest(
        @Schema(description = "Текст open-ended ответа (для соответствующего типа вопроса)")
        String openAnswer,
        @ArraySchema(schema = @Schema(description = "Выбранный вариант ответа"))
        List<String> selectedAnswers,
        @Schema(description = "Ответы по индексам вопросов: questionIndex -> список ответов")
        Map<Integer, List<String>> questionAnswers
) {
}
