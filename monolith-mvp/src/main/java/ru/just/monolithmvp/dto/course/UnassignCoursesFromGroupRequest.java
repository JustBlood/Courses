package ru.just.monolithmvp.dto.course;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UnassignCoursesFromGroupRequest(
        @ArraySchema(schema = @Schema(description = "ID курсов для снятия с группы", example = "550e8400-e29b-41d4-a716-446655440000"))
        @NotEmpty List<Long> courseIds,
        @NotNull Boolean deleteProgress
) {
}
