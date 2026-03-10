package ru.just.monolithmvp.dto.lesson;

import jakarta.validation.constraints.Min;
import ru.just.monolithmvp.model.LessonType;

public record UpdateTheoryLessonRequest(
        @Min(1) Integer position,
        String title,
        String description,
        String coverFilePath,
        Boolean requiresPreviousCompleted,
        Boolean openForAccess,
        Boolean stopLesson,
        Boolean blockedDuringAttempt,
        @Min(1) Integer attemptLimit,
        @Min(1) Integer timeLimitMinutes,
        LessonType lessonType,
        String content,
        @Min(0) Integer fullPoints
) {
}
