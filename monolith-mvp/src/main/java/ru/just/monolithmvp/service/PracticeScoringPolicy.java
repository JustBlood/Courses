package ru.just.monolithmvp.service;

import org.springframework.stereotype.Service;
import ru.just.monolithmvp.model.PracticeQuestion;
import ru.just.monolithmvp.model.QuestionPointsType;
import ru.just.monolithmvp.model.QuestionType;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class PracticeScoringPolicy {
    public int scoreQuestion(PracticeQuestion question, List<String> selectedAnswers, List<String> correctAnswers) {
        return switch (resolveTestQuestionPointsType(question, selectedAnswers, correctAnswers)) {
            case FULL -> question.getFullPoints() == null ? 0 : question.getFullPoints();
            case PARTIAL -> question.getPartialPoints() == null ? 0 : question.getPartialPoints();
            case ZERO -> 0;
        };
    }

    public QuestionPointsType resolveTestQuestionPointsType(PracticeQuestion question,
                                                            List<String> selectedAnswers,
                                                            List<String> correctAnswers) {
        if (evaluateCorrectness(question.getQuestionType(), selectedAnswers, correctAnswers)) {
            return QuestionPointsType.FULL;
        }

        if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            Set<String> selectedSet = new HashSet<>(selectedAnswers);
            Set<String> correctSet = new HashSet<>(correctAnswers);

            long wrongSelected = selectedSet.stream().filter(answer -> !correctSet.contains(answer)).count();
            long missedCorrect = correctSet.stream().filter(answer -> !selectedSet.contains(answer)).count();

            if (wrongSelected <= 1 && missedCorrect <= 1 && Optional.ofNullable(question.getOptions()).orElse(List.of()).size() != 3) {
                return QuestionPointsType.PARTIAL;
            }
        }

        return QuestionPointsType.ZERO;
    }

    public QuestionPointsType resolveOpenQuestionPointsType(int awardedPoints, int fullPoints) {
        if (awardedPoints <= 0) {
            return QuestionPointsType.ZERO;
        }
        if (awardedPoints >= fullPoints) {
            return QuestionPointsType.FULL;
        }
        return QuestionPointsType.PARTIAL;
    }

    private boolean evaluateCorrectness(QuestionType type, List<String> selected, List<String> correct) {
        if (type == QuestionType.ORDERING) {
            return Objects.equals(selected, correct);
        }
        return new HashSet<>(selected).equals(new HashSet<>(correct));
    }
}
