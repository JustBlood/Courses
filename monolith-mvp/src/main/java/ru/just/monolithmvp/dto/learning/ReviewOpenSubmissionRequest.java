package ru.just.monolithmvp.dto.learning;

public record ReviewOpenSubmissionRequest(
        boolean passed,
        String comment
) {
}
