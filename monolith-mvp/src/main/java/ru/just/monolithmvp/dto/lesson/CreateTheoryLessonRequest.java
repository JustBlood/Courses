package ru.just.monolithmvp.dto.lesson;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.just.monolithmvp.model.TheoryContentType;

public record CreateTheoryLessonRequest(
        @NotNull Integer position,
        @NotBlank String title,
        @NotNull TheoryContentType contentType,
        @NotBlank String content
) {
}
