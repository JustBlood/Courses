package ru.just.monolithmvp.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecoverPasswordRequest(
        @Email @NotBlank String email
) {
}
