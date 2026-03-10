package ru.just.monolithmvp.dto.course;

public record CourseDto(
        Long id,
        String title,
        String description,
        String authorFullName,
        String coverFilePath,
        Integer passingThresholdPercent,
        Integer deadlineDays,
        Boolean lessonsFreeOrder,
        Boolean blockAfterDeadline,
        Boolean includeInOverallStats,
        Long sectionId,
        String sectionTitle,
        Integer sectionPriority
) {
}
