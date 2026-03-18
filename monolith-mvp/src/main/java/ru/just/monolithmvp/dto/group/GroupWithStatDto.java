package ru.just.monolithmvp.dto.group;

import ru.just.monolithmvp.model.GroupType;

import java.util.UUID;

public record GroupWithStatDto (
    UUID id,
    String title,
    GroupType type,
    Long studentsCount,
    Long coursesCount,
    Long programsCount
) {
}
