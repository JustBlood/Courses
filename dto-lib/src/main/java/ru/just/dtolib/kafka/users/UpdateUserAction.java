package ru.just.dtolib.kafka.users;

import lombok.*;

@Builder
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class UpdateUserAction {
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String photoUrl;
}
