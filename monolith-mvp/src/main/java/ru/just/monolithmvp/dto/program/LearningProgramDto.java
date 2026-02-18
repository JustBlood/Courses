package ru.just.monolithmvp.dto.program;

import ru.just.monolithmvp.model.ProgramAccessCondition;

import java.util.List;

public record LearningProgramDto(
        Long id,
        String title,
        String description,
        String coverFilePath,
        ProgramAccessCondition accessCondition,
        List<LearningProgramCourseDto> courses
) {
}