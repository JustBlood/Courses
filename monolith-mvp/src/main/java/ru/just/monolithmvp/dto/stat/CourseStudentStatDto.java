package ru.just.monolithmvp.dto.stat;

import ru.just.monolithmvp.model.CourseProgressStatus;

public record CourseStudentStatDto(
        Long studentId,
        String fullName,
        String email,
        String username,
        Integer earnedPoints,
        Integer maxPoints,
        Integer efficiencyPercent,
        Integer progressPercent,
        Long completedLessons,
        Long totalLessons,
        CourseProgressStatus status,
        String enrolledAt,
        String startedAt,
        String completedAt
) {
}
