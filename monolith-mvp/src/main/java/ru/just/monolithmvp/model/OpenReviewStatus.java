package ru.just.monolithmvp.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OpenReviewStatus {
    PENDING_REVIEW(false),
    ACCEPTED(true),
    REWORK(false),
    REJECTED(true);

    private final boolean isFinal;
}

