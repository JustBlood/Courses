package ru.just.monolithmvp.dto.program;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ProgramCourseSettingsRequest(
        @NotNull Long courseId
) {
}
