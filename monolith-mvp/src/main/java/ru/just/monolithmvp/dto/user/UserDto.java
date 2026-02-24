package ru.just.monolithmvp.dto.user;

import ru.just.monolithmvp.model.Role;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String fullName,
        String email,
        Role role,
        boolean enabled,
        boolean activated,
        String phone,
        String comment,
        String avatarFilePath,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime lastVisit,
        LocalDateTime deactivatedAt,
        String deactivatedBy
) {
}
