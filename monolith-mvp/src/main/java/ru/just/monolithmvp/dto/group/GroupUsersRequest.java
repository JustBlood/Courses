package ru.just.monolithmvp.dto.group;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GroupUsersRequest(
        @NotEmpty List<Long> userIds
) {
}
