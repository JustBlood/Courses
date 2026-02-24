package ru.just.monolithmvp.dto.common;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(description = "Запрос со списком UUID идентификаторов")
public record UuidIdsRequest(
        @ArraySchema(schema = @Schema(description = "UUID сущности", example = "550e8400-e29b-41d4-a716-446655440000"))
        @NotEmpty List<UUID> ids
) {
}
