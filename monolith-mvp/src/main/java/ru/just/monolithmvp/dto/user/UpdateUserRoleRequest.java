package ru.just.monolithmvp.dto.user;

import jakarta.validation.constraints.NotNull;
import ru.just.monolithmvp.model.Role;

public record UpdateUserRoleRequest(
        @NotNull Role role
) {
}
