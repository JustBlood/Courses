package ru.just.monolithmvp.service;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.just.monolithmvp.model.PracticeQuestion;
import ru.just.monolithmvp.model.QuestionPointsType;
import ru.just.monolithmvp.model.QuestionType;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class PracticeScoringPolicy {

    public int scoreQuestion(QuestionPointsType pointsType, PracticeQuestion question) {
        return switch (pointsType) {
            case FULL -> question.getFullPoints() == null ? 0 : question.getFullPoints();
            case PARTIAL -> question.getPartialPoints() == null ? 0 : question.getPartialPoints();
            case ZERO -> 0;
        };
    }

    public int scoreQuestion(PracticeQuestion question, List<String> selectedAnswers, List<String> correctAnswers) {
        return scoreQuestion(resolveTestQuestionPointsType(question, selectedAnswers, correctAnswers), question);
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

            if (!CollectionUtils.isEmpty(selectedAnswers) && wrongSelected <= 1 && missedCorrect <= 1) {
                return QuestionPointsType.PARTIAL;
            }
        }

        return QuestionPointsType.ZERO;
    }

    private boolean evaluateCorrectness(QuestionType type, List<String> selected, List<String> correct) {
        if (type == QuestionType.ORDERING) {
            return Objects.equals(selected, correct);
        }
        return new HashSet<>(selected).equals(new HashSet<>(correct));
    }
}
