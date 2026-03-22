package ru.just.monolithmvp.dto.stat;

import java.time.LocalDateTime;

public record ReportRowDto(
        String fullName,
        String snils,
        Long courseId,
        String courseTitle,
        Integer earnedPoints,
        Integer maxPoints,
        Integer efficiencyPercent,
        LocalDateTime enrolledAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}
