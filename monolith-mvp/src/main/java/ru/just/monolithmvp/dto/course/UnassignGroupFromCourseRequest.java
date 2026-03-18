package ru.just.monolithmvp.dto.course;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UnassignGroupFromCourseRequest(
        @ArraySchema(schema = @Schema(description = "UUID групп для снятия курса", example = "550e8400-e29b-41d4-a716-446655440000"))
        @NotEmpty List<UUID> groupIds,
        @NotNull Boolean deleteProgress
) {
}
