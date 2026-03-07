package ru.just.monolithmvp.dto.lesson;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.just.monolithmvp.model.TheoryContentType;

import java.util.Map;

public record CreateTheoryLessonRequest(
        @Min(1) Integer position,
        @NotBlank String title,
        String description,
        String coverFilePath,
        Boolean requiresPreviousCompleted,
        Boolean openForAccess,
        Boolean stopLesson,
        Boolean blockedDuringAttempt,
        @Min(1) Integer attemptLimit,
        @Min(1) Integer timeLimitMinutes,
        @NotNull TheoryContentType contentType,
        @NotBlank String content,
        @Min(0) Integer fullPoints,
        Map<Long, Long> questionIdToPosition
) {
}
