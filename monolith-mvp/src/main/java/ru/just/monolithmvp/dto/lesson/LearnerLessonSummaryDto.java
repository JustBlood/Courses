package ru.just.monolithmvp.dto.lesson;

import ru.just.monolithmvp.model.LessonType;

public record LearnerLessonSummaryDto(
        Long id,
        Integer position,
        String title,
        LessonType lessonType,
        Boolean blocked,
        String blockReason,
        Integer fullPoints,
        LessonProgressDto lessonProgress
) {
}
