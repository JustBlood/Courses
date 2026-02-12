package ru.just.monolithmvp.dto.learning;

import java.util.List;

public record PracticeSubmissionRequest(
        String openAnswer,
        List<String> selectedAnswers
) {
}
