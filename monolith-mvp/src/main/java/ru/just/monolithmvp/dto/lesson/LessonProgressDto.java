package ru.just.monolithmvp.dto.lesson;

import ru.just.monolithmvp.model.SubmissionStatus;

public record LessonProgressDto(
        Boolean completed,
        SubmissionStatus status,
        Integer pointsAwarded
) {
}
