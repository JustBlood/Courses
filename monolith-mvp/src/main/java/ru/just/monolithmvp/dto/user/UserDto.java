package ru.just.monolithmvp.dto.user;

import ru.just.monolithmvp.model.Role;

public record UserDto(
        Long id,
        String username,
        Role role,
        boolean enabled
) {
}
