package ru.just.monolithmvp.dto.program;

import ru.just.monolithmvp.model.ProgramAccessCondition;

import java.time.LocalDateTime;
import java.util.List;

public record ProgramDto(
        Long id,
        String title,
        String description,
        ProgramAccessCondition accessCondition,
        LocalDateTime deadlineAt,
        Boolean blockAfterDeadline,
        Boolean completed,
        List<ProgramCourseDto> courses
) {
}
