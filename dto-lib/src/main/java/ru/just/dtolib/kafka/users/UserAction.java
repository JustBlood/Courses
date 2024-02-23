package ru.just.dtolib.kafka.users;

import lombok.Builder;

@Builder
public class UserAction {
    private Long userId;
    private UserActionType userActionType;
    public enum UserActionType {
        CREATED, DELETED
    }
}
