package ru.just.monolithmvp.dto.lesson;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.just.monolithmvp.model.LessonType;

import java.util.Map;

public record CreateTheoryLessonRequest(
        @NotBlank String title,
        String description,
        Boolean stopLesson,
        @Min(1) Integer timeLimitMinutes,
        @NotNull LessonType lessonType,
        @NotBlank String content,
        @Min(0) Integer fullPoints,
        Map<Long, Long> questionIdToPosition
) {
}
