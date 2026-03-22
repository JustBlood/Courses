package ru.just.monolithmvp.dto.stat;

public record ReportRowDto(
        String fullName,
        String email,
        String login,
        String lang,
        String courseTitle,
        Integer earnedPoints,
        Integer maxPoints,
        Integer efficiencyPercent,
        String enrolledAt,
        String startedAt,
        String completedAt
) {
}
