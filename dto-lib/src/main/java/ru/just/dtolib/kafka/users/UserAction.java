package ru.just.dtolib.kafka.users;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAction {
    private Long userId;
    private String login;
    private UserActionType userActionType;
    public enum UserActionType {
        CREATED, DELETED
    }
}
