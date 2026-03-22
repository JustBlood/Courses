package ru.just.monolithmvp.dto.program;

public record ProgramCourseDto(
        Long courseId,
        String title,
        String description,
        String coverFilePath,
        Long deadlineAt,
        Long deadlineDays,
        Integer orderIndex,
        Boolean available,
        Boolean viewed,
        Boolean completed
) {
}
