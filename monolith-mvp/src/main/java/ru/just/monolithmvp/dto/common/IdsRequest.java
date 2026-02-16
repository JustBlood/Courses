package ru.just.monolithmvp.dto.common;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record IdsRequest(
        @NotEmpty List<Long> ids
) {
}
