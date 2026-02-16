package ru.just.monolithmvp.dto.stat;

public record StudentCourseStatDto(
        Long courseId,
        String courseTitle,
        Integer earnedPoints,
        Integer maxPoints,
        Double efficiencyPercent,
        Long completedLessons,
        Long totalLessons,
        String enrolledAt,
        String startedAt,
        String completedAt
) {
}
