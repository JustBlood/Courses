package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.AppUser;
import ru.just.monolithmvp.model.Role;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    Optional<AppUser> findByEmail(String email);
    List<AppUser> findAllByIdNotInAndActivation(List<Long> ids, Boolean activation);
    List<AppUser> findAllByIdInAndActivation(List<Long> ids, Boolean activation);
    List<AppUser> findAllByRole(Role role);
}
