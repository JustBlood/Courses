package ru.just.monolithmvp.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.just.monolithmvp.model.Role;

@Schema(description = "Ответ успешной аутентификации")
public record LoginResponse(
        @Schema(description = "JWT access token")
        String token,
        @Schema(description = "ID пользователя")
        Long userId,
        @Schema(description = "Email пользователя")
        String email,
        @Schema(description = "Роль пользователя")
        Role role
) {
}
