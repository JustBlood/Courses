package ru.just.monolithmvp.dto.lesson;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.just.monolithmvp.model.LessonType;

import java.util.List;

public record CreatePracticeLessonRequest(
        @Min(1) Integer position,
        @NotBlank String title,
        String description,
        Boolean stopLesson,
        @Min(1) Integer attemptLimit,
        @Min(1) Integer timeLimitMinutes,
        @NotNull LessonType lessonType,
        @Min(0) Integer fullPoints,
        @Min(0) @jakarta.validation.constraints.Max(100) Integer passingThresholdPercent,
        Boolean shuffleOptions,
        Boolean showQuestionStatus,
        Boolean showCorrectAnswers,
        @NotEmpty List<@Valid PracticeQuestionRequest> questions
) {
}
