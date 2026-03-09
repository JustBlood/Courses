package ru.just.monolithmvp.dto.lesson;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import ru.just.monolithmvp.model.LessonType;

import java.util.List;

public record UpdatePracticeLessonRequest(
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
        @Min(0) Integer fullPoints,
        @Min(0) @Max(100) Integer passingThresholdPercent,
        Boolean evaluateByCorrectCount,
        @Min(1) Integer randomQuestionCount,
        Boolean shuffleOptions,
        Boolean showQuestionStatus,
        Boolean showCorrectAnswers,
        List<@Valid PracticeQuestionRequest> questions
) {
}
