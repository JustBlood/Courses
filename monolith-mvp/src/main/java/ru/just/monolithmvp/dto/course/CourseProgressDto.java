package ru.just.monolithmvp.dto.course;

import ru.just.monolithmvp.model.CourseProgressStatus;

public record CourseProgressDto(
        Long deadlineAt,
        Integer completionPercent,
        Integer completedLessons,
        Integer remainingLessons,
        CourseProgressStatus completionStatus
) {
}
