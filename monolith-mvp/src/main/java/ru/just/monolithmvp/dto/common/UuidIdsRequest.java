package ru.just.monolithmvp.dto.common;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record UuidIdsRequest(
        @NotEmpty List<UUID> ids
) {
}
