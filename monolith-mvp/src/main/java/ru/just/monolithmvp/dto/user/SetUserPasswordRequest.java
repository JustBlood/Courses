package ru.just.monolithmvp.dto.user;

import jakarta.validation.constraints.NotBlank;

public record SetUserPasswordRequest(
        @NotBlank String password
) {
}
