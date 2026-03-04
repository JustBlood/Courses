package ru.just.monolithmvp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на смену пароля в авторизованной сессии")
public record ChangePasswordRequest(
        @Schema(description = "Текущий пароль", example = "OldPassword123")
        @NotBlank String currentPassword,
        @Schema(description = "Новый пароль", example = "NewStrongPassword123")
        @NotBlank String newPassword
) {
}
