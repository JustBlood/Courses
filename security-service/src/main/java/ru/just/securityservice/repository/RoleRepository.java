package ru.just.securityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.securityservice.model.Role;
import ru.just.securityservice.model.User;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
