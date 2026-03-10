package ru.just.monolithmvp.dto.course;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CreateCourseRequest(
        @NotBlank String title,
        String description,
        String authorFullName,
        String coverFilePath,
        @Min(0) @Max(100) Integer passingThresholdPercent,
        @Min(1) Integer deadlineDays,
        Boolean lessonsFreeOrder,
        Boolean blockAfterDeadline,
        Boolean includeInOverallStats,
        Long sectionId,
        Map<Long, Integer> lessonIdToPosition
) {
}
