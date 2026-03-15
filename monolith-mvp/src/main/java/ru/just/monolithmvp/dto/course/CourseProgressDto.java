package ru.just.monolithmvp.dto.course;

import ru.just.monolithmvp.model.CourseProgressStatus;

import java.time.LocalDateTime;

public record CourseProgressDto(
        LocalDateTime deadlineAt,
        Integer completionPercent,
        Integer completedLessons,
        Integer remainingLessons,
        CourseProgressStatus completionStatus
) {
}
