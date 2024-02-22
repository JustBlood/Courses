package ru.just.securityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.securityservice.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
}
