package ru.just.securityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.securityservice.model.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByNameEndsWith(String roleName);
}
