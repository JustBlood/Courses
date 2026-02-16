package ru.just.monolithmvp.dto.user;

import ru.just.monolithmvp.model.Role;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String fullName,
        String email,
        String username,
        Role role,
        boolean enabled,
        String lang,
        String phone,
        String comment,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime lastVisit,
        LocalDateTime deactivatedAt,
        String deactivatedBy
) {
}
