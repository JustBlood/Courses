package ru.just.monolithmvp.dto.course;

import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(
        @NotBlank String title,
        String description
) {
}
