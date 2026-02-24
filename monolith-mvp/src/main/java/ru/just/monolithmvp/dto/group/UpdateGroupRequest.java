package ru.just.monolithmvp.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.just.monolithmvp.model.GroupType;

public record UpdateGroupRequest(
        @NotBlank String title,
        @NotNull GroupType type
) {
}