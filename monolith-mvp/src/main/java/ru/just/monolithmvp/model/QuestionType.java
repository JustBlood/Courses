package ru.just.monolithmvp.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum QuestionType {
    SINGLE_CHOICE(false),
    MULTIPLE_CHOICE(false),
    ORDERING(false),
    OPEN_ANSWER(true);

    private final Boolean needToReview;

    public static final List<QuestionType> TEST_QUESTIONS = List.of(SINGLE_CHOICE, MULTIPLE_CHOICE, ORDERING);
    public static final List<QuestionType> OPEN_ANSWERS = List.of(OPEN_ANSWER);
}
