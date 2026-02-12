package ru.just.monolithmvp.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.just.monolithmvp.model.Role;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotNull Role role
) {
}
