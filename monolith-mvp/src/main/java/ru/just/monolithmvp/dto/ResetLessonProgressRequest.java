package ru.just.monolithmvp.dto;

import jakarta.validation.constraints.NotNull;

public record ResetLessonProgressRequest(
        @NotNull Long courseId,
        @NotNull Long lessonId
) {
}
