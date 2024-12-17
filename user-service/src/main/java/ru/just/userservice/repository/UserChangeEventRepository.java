package ru.just.userservice.repository;


import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.just.dtolib.audit.ChangeType;
import ru.just.userservice.audit.UserChangeEvent;

import java.util.Optional;

import static ru.just.model.Tables.USER_CHANGE_EVENT;

@RequiredArgsConstructor
@Repository
public class UserChangeEventRepository {
    private final DSLContext jooq;

    public Optional<UserChangeEvent> findByUserIdAndChangeType(Long userId, ChangeType changeType) {
        throw new IllegalStateException("not available");
    }

    public void save(UserChangeEvent withChangeType) {
        jooq.insertInto(USER_CHANGE_EVENT)
                .set(USER_CHANGE_EVENT.USER_ID, withChangeType.getUserId())
                .set(USER_CHANGE_EVENT.CHANGE_TYPE, withChangeType.getChangeType().name())
                .set(USER_CHANGE_EVENT.AUTHOR_ID, withChangeType.getAuthorId())
                .execute();
    }
}
