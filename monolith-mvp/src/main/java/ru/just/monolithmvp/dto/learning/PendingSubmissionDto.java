package ru.just.monolithmvp.dto.learning;

public record PendingSubmissionDto(
        Long submissionId,
        Long lessonId,
        String lessonTitle,
        Long studentId,
        String studentUsername,
        String answer,
        String submittedAt
) {
}
