package ru.just.userservice.audit;

import lombok.*;
import ru.just.dtolib.audit.ChangeType;

import java.time.LocalDateTime;

@With
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserChangeEvent {
    private Long id;
    private ChangeType changeType;
    private Long authorId;
    private LocalDateTime changeTime = LocalDateTime.now();
    private Long userId;
}
