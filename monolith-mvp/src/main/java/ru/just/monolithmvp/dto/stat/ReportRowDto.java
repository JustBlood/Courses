package ru.just.monolithmvp.dto.stat;

public record ReportRowDto(
        String fullName,
        String email,
        String username,
        String courseTitle,
        Integer earnedPoints,
        Integer maxPoints,
        Double efficiencyPercent,
        String enrolledAt,
        String startedAt,
        String completedAt
) {
}
