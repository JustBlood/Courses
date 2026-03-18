package ru.just.monolithmvp.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import ru.just.monolithmvp.model.Role;

public record UpdateUserRequest(
        @Size(max = 255) String fullName,
        @Email @Size(max = 255) String email,
        Role role,
        String avatarFilePath,
        @Size(max = 255) String phone,
        String snils,
        @Size(max = 2000) String comment,
        String password
) {
}
