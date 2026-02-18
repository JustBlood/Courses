package ru.just.monolithmvp.dto.program;

import jakarta.validation.constraints.NotNull;

public record GroupAssignmentRequest(
        @NotNull ProgramTargetType targetType,
        @NotNull Long targetId
) {
}