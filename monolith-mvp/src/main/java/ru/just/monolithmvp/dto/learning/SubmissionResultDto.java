package ru.just.monolithmvp.dto.learning;

import ru.just.monolithmvp.model.SubmissionStatus;

public record SubmissionResultDto(
        Long submissionId,
        SubmissionStatus status,
        boolean passed,
        String message
) {
}
