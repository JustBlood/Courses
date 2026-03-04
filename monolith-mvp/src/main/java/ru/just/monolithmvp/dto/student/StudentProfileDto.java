package ru.just.monolithmvp.dto.student;

import ru.just.monolithmvp.dto.group.GroupDto;
import ru.just.monolithmvp.dto.user.UserDto;

import java.util.List;

public record StudentProfileDto(
        UserDto user,
        List<GroupDto> groups
) {
}