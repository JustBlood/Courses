package ru.just.monolithmvp.dto.learning;

public record ReviewOpenSubmissionRequest(
        boolean passed,
        boolean partialPoints,
        boolean toNextReview,
        String comment
) {
}
