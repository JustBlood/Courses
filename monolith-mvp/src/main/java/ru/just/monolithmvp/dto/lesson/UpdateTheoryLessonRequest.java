package ru.just.monolithmvp.dto.lesson;

import jakarta.validation.constraints.Min;
import ru.just.monolithmvp.model.TheoryContentType;

public record UpdateTheoryLessonRequest(
        String title,
        String description,
        String coverFilePath,
        Boolean requiresPreviousCompleted,
        Boolean openForAccess,
        Boolean stopLesson,
        Boolean blockedDuringAttempt,
        @Min(1) Integer attemptLimit,
        @Min(1) Integer timeLimitMinutes,
        TheoryContentType contentType,
        String content,
        @Min(0) Integer fullPoints
) {
}
