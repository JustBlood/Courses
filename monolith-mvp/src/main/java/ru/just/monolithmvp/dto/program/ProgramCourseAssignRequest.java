package ru.just.monolithmvp.dto.program;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record ProgramCourseAssignRequest(
        @NotEmpty List<Long> orderedCourseIds
) {
}
