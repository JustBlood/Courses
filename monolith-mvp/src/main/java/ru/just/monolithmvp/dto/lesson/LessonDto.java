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
        String coverFilePath,
        boolean requiresPreviousCompleted,
        boolean openForAccess,
        Boolean stopLesson,
        Boolean blockedDuringAttempt,
        Integer attemptLimit,
        Integer timeLimitMinutes,
        LessonType lessonType,
        TheoryContentType theoryContentType,
        String theoryContent,
        Integer fullPoints,
        Integer partialPoints,
        Integer passingThresholdPercent,
        Boolean evaluateByCorrectCount,
        Integer randomQuestionCount,
        Boolean shuffleOptions,
        Boolean showQuestionStatus,
        Boolean showCorrectAnswers,
        List<PracticeQuestionDto> questions
) {
}
