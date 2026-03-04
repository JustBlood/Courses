package ru.just.monolithmvp.dto.course;

import ru.just.monolithmvp.dto.user.UserDto;

import java.util.List;

public record UserInNotInListsDto(
        List<UserDto> in,
        List<UserDto> notIn
) {
}
