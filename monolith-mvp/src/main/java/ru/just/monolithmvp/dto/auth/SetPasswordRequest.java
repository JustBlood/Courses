package ru.just.monolithmvp.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record SetPasswordRequest(
        @NotBlank String password
) {
}
