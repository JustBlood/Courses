package ru.just.monolithmvp.dto.user;

import ru.just.monolithmvp.model.Role;

public record UserDto(
        Long id,
        String fullName,
        String email,
        Role role,
        boolean activation,
        boolean enabled,
        String phone,
        String comment,
        String avatarFilePath,
        Long createdAt,
        String createdBy,
        Long lastVisit,
        Long deactivatedAt,
        String deactivatedBy
) {
}
