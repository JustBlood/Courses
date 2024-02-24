package ru.just.securityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.dtolib.kafka.users.UserDeliverStatus;
import ru.just.securityservice.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginAndDeliverStatus(String login, UserDeliverStatus deliverStatus);
    boolean existsByLoginOrEmail(String login, String email);
}
