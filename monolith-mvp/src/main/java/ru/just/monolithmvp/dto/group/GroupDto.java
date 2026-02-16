package ru.just.monolithmvp.dto.group;

import ru.just.monolithmvp.model.GroupType;

import java.util.UUID;

public record GroupDto(
        UUID id,
        String title,
        GroupType type
) {
}
