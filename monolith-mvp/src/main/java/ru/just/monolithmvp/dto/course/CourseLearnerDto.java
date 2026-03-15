package ru.just.monolithmvp.dto.course;

import ru.just.monolithmvp.dto.lesson.LearnerLessonSummaryDto;
import ru.just.monolithmvp.model.CourseProgressStatus;

import java.time.LocalDateTime;
import java.util.List;

public record CourseLearnerDto(
        Long id,
        String title,
        String description,
        String coverFilePath,
        Integer deadlineDays,
        Integer totalLessons,
        CourseProgressDto progress,
        List<LearnerLessonSummaryDto> lessons
) {
}
