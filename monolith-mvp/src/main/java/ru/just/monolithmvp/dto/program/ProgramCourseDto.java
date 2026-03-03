package ru.just.monolithmvp.dto.program;

public record ProgramCourseDto(
        Long courseId,
        Integer orderIndex,
        Boolean available,
        Boolean viewed,
        Boolean completed
) {
}
