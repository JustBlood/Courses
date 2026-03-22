package ru.just.monolithmvp.dto.lesson;

import lombok.Builder;
import ru.just.monolithmvp.model.LessonType;
import ru.just.monolithmvp.model.SubmissionStatus;

import java.util.List;

@Builder
public record LearnerLessonDto (
    Long id,
    Integer position,
    String title,
    String description,
    LessonType lessonType,
    String theoryContent,
    Long deadlineAt,
    Integer timeLimitMinutes,
    SubmissionStatus status,
    Integer attempts,
    Integer maxAttempts,
    List<LearnerPracticeQuestionDto> questions
) {

}
