package ru.just.monolithmvp.dto.course;

public record CourseSummaryDto(
        Long id,
        String title,
        String description,
        String coverFilePath,
        Long sectionId,
        String sectionTitle,
        Integer sectionPriority,
        long theoryLessonsCount,
        long practiceLessonsCount
) {
}
