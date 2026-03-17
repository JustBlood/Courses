package ru.just.monolithmvp.model;

import java.util.List;

public enum SubmissionStatus {
    COMPLETED,
    INCOMPLETED,
    PENDING_REVIEW,
    REWORKING,
    STARTED;

    public static final List<SubmissionStatus> FINAL_STATUSES = List.of(COMPLETED, INCOMPLETED);
    public static final List<SubmissionStatus> ALLOW_GET_NEXT_LESSON_STATUSES = List.of(COMPLETED, PENDING_REVIEW);
}
