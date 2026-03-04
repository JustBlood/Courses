package ru.just.monolithmvp.dto.user;

import ru.just.monolithmvp.model.Role;

public record UpdateUserRequest(
        String fullName,
        String email,
        Role role,
        String phone,
        String comment,
        String password
) {
}
