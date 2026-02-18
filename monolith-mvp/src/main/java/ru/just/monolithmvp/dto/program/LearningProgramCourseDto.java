package ru.just.monolithmvp.dto.program;

import java.time.LocalDateTime;

public record LearningProgramCourseDto(
        Long courseId,
        String courseTitle,
        Integer orderIndex,
        LocalDateTime deadlineAt,
        boolean blockAfterDeadline,
        boolean available,
        boolean completed,
        boolean viewedOrPending
) {
}