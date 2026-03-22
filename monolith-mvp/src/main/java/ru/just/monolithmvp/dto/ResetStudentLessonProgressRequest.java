package ru.just.monolithmvp.dto;

import jakarta.validation.constraints.NotNull;

public record ResetStudentLessonProgressRequest(
        @NotNull Long userId,
        @NotNull Long courseId,
        @NotNull Long lessonId
) {
}
