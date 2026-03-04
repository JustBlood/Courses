package ru.just.monolithmvp.dto.lesson;

public record LessonSummaryDto(
        Long id,
        Integer position,
        String title,
        String description,
        String coverFilePath,
        boolean requiresPreviousCompleted,
        boolean available
) {
}