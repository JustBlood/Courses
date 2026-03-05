package ru.just.monolithmvp.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.just.monolithmvp.model.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotNull Role role,
        String avatarFilePath,
        String phone,
        String comment,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime lastVisit,
        LocalDateTime deactivatedAt,
        String deactivatedBy,
        String password,
        List<UUID> groupIds,
        List<Long> courseIds
) {
}
