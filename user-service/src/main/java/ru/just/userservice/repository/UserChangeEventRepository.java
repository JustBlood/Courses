package ru.just.userservice.repository;


import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.just.dtolib.audit.ChangeType;
import ru.just.userservice.audit.UserChangeEvent;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class UserChangeEventRepository {
    private final DSLContext jooq;

    public Optional<UserChangeEvent> findByUserIdAndChangeType(Long userId, ChangeType changeType) {
        throw new IllegalStateException("not available");
    }

    public void save(UserChangeEvent withChangeType) {

    }
}
