package ru.just.monolithmvp.dto.auth;

import ru.just.monolithmvp.model.Role;

public record LoginResponse(
        String token,
        Long userId,
        String email,
        Role role
) {
}
