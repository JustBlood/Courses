package ru.just.monolithmvp.dto.program;

import ru.just.monolithmvp.model.ProgramAccessCondition;

import java.util.List;

public record ProgramDto(
        Long id,
        String title,
        String description,
        ProgramAccessCondition accessCondition,
        Long deadlineDays,
        Long deadlineAt,
        Boolean blockAfterDeadline,
        Boolean completed,
        List<ProgramCourseDto> courses
) {
}
