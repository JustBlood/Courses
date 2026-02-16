package ru.just.monolithmvp.dto.course;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(
        @NotBlank String title,
        String description,
        String coverFilePath,
        @Min(0) @Max(100) Integer passingThresholdPercent
) {
}
