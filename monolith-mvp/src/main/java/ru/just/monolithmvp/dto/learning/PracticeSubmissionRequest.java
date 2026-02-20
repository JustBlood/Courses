package ru.just.monolithmvp.dto.learning;

import java.util.List;
import java.util.Map;

public record PracticeSubmissionRequest(
        String openAnswer,
        List<String> selectedAnswers,
        Map<Integer, List<String>> questionAnswers
) {
}
