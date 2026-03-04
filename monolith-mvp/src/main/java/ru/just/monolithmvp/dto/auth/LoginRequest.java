package ru.just.monolithmvp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос аутентификации пользователя")
public record LoginRequest(
        @Schema(description = "Email пользователя", example = "student@company.local")
        @NotBlank String email,
        @Schema(description = "Пароль пользователя", example = "StrongPassword123")
        @NotBlank String password
) {
}
