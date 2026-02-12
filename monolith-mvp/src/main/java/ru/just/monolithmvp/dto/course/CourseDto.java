package ru.just.monolithmvp.dto.course;

public record CourseDto(
        Long id,
        String title,
        String description,
        Long createdByAdminId
) {
}
