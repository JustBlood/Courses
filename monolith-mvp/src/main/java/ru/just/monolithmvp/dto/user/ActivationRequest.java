package ru.just.monolithmvp.dto.user;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ActivationRequest (
    Boolean activate,
    @NotNull List<Long> userIds
) {}
