package ru.just.monolithmvp.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LessonType {
    THEORY_TEXT(LessonSubType.THEORY),
    THEORY_VIDEO(LessonSubType.THEORY),
    THEORY_PDF(LessonSubType.THEORY),
    PRACTICE_TEST(LessonSubType.PRACTICE),
    PRACTICE_OPEN_ANSWER(LessonSubType.PRACTICE);

    private final LessonSubType subType;

    public enum LessonSubType {
        PRACTICE, THEORY;
    }
}
