package ru.just.courses.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.courses.model.audit.ChangeType;
import ru.just.courses.model.audit.UserChangeEvent;

import java.util.Optional;

public interface UserChangeEventRepository extends JpaRepository<UserChangeEvent, Long> {
    Optional<UserChangeEvent> findByUserIdAndChangeType(Long userId, ChangeType changeType);
}
