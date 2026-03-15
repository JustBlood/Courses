package ru.just.monolithmvp.dto.lesson;

import ru.just.monolithmvp.model.LessonType;

import java.util.List;

public record LessonDto(
        Long id,
        Long courseId,
        Integer position,
        String title,
        String description,
        Boolean stopLesson,
        Integer attemptLimit,
        Integer timeLimitMinutes,
        LessonType lessonType,
        String theoryContent,
        Integer fullPoints,
        Integer passingThresholdPercent,
        Boolean shuffleOnEveryAttempt,
        Boolean showCorrectAnswersAfterCompletion,
        List<PracticeQuestionDto> questions
) {
}
