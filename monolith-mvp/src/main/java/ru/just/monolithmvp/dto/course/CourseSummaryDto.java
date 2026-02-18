package ru.just.monolithmvp.dto.course;

public record CourseSummaryDto(
        Long id,
        String title,
        String description,
        String coverFilePath,
        long theoryLessonsCount,
        long practiceLessonsCount
) {
}
