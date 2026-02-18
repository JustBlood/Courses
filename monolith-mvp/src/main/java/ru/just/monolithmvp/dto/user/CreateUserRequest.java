package ru.just.monolithmvp.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.just.monolithmvp.model.Role;

import java.time.LocalDateTime;

public record CreateUserRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotNull Role role,
        String customId,
        String phone,
        String comment,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime lastVisit,
        LocalDateTime deactivatedAt,
        String deactivatedBy
) {
}
