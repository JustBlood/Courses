package ru.just.monolithmvp.dto.stat;

public record CourseStudentStatDto(
        Long studentId,
        String fullName,
        String email,
        String username,
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
