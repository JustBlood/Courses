package ru.just.monolithmvp.dto.stat;

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
        String enrolledAt,
        String startedAt,
        String completedAt
) {
}
