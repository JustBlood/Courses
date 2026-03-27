package ru.just.monolithmvp.dto.lesson;

import jakarta.validation.constraints.Min;
import ru.just.monolithmvp.model.LessonType;

public record UpdateTheoryLessonRequest(
        @Min(1) Integer position,
        String title,
        String description,
        Boolean stopLesson,
        @Min(1) Integer timeLimitMinutes,
        LessonType lessonType,
        String content,
        @Min(0) Integer fullPoints
) {
}
