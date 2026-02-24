package ru.just.monolithmvp.dto.section;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateSectionRequest(
        @NotBlank String title,
        String description,
        @NotNull @Min(0) Integer priority
) {
}
