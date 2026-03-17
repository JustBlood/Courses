package ru.just.monolithmvp.dto.course;

public record CourseDto(
        Long id,
        String title,
        String description,
        String authorFullName,
        String coverFilePath,
        Integer deadlineDays,
        Boolean lessonsFreeOrder,
        Long sectionId,
        String sectionTitle,
        Integer sectionPriority
) {
}
