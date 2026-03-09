package ru.just.monolithmvp.dto.lesson;

import ru.just.monolithmvp.model.LessonType;
import ru.just.monolithmvp.model.TheoryContentType;

import java.util.List;

public record LessonDto(
        Long id,
        Long courseId,
        Integer position,
        String title,
        String description,
        Boolean stopLesson,
        Boolean blockedDuringAttempt,
        Integer attemptLimit,
        Integer timeLimitMinutes,
        LessonType lessonType,
        TheoryContentType theoryContentType,
        String theoryContent,
        Integer fullPoints,
        Integer passingThresholdPercent,
        Boolean evaluateByCorrectCount,
        Integer randomQuestionCount,
        Boolean shuffleOnEveryAttempt,
        Boolean showCorrectAnswersAfterCompletion,
        List<PracticeQuestionDto> questions
) {
}
