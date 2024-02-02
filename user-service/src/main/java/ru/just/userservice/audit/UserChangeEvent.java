package ru.just.userservice.audit;

import lombok.*;
import ru.just.dtolib.audit.ChangeType;

import java.time.ZonedDateTime;

@With
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserChangeEvent {
    private Long id;
    private ChangeType changeType;
    private Long authorId;
    private ZonedDateTime changeTime = ZonedDateTime.now();
    private Long userId;
}
