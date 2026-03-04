package ru.just.monolithmvp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос установки пароля по токену")
public record SetPasswordRequest(
        @Schema(description = "Новый пароль", example = "NewStrongPassword123")
        @NotBlank String password
) {
}
