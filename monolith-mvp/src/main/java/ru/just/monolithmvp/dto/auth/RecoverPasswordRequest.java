package ru.just.monolithmvp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на восстановление пароля")
public record RecoverPasswordRequest(
        @Schema(description = "Email пользователя для восстановления", example = "user@company.local")
        @Email @NotBlank String email
) {
}
