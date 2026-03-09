package ru.just.monolithmvp.dto;

import jakarta.validation.constraints.NotNull;

public record ResetStudentProgressRequest(
        @NotNull Long userId,
        @NotNull Long courseId
) {
}
