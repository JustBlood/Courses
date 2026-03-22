package ru.just.monolithmvp.dto.course;

import ru.just.monolithmvp.dto.lesson.LearnerLessonSummaryDto;

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
