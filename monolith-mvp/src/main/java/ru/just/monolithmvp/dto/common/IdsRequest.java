package ru.just.monolithmvp.dto.common;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Запрос со списком числовых идентификаторов")
public record IdsRequest(
        @ArraySchema(schema = @Schema(description = "ID сущности", example = "1"))
        @NotEmpty List<Long> ids
) {
}
