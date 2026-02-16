package ru.just.monolithmvp.dto.group;

import ru.just.monolithmvp.dto.user.UserDto;
import ru.just.monolithmvp.model.GroupType;

import java.util.List;
import java.util.UUID;

public record GroupUsersDto(
        UUID id,
        String title,
        GroupType type,
        List<UserDto> users
) {
}