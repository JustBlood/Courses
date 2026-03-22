package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.just.monolithmvp.model.AppUser;
import ru.just.monolithmvp.model.GroupType;
import ru.just.monolithmvp.model.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    Optional<AppUser> findByEmail(String email);
    List<AppUser> findAllByIdNotInAndActivation(List<Long> ids, Boolean activation);
    List<AppUser> findAllByIdInAndActivation(List<Long> ids, Boolean activation);
    List<AppUser> findAllByRole(Role role);

    @Query("""
        select u
        from AppUser u
        where u.activation = true and not exists (
            select 1
            from GroupMembership gm
            where gm.user = u and gm.group.type = :type
        )
        """)
    List<AppUser> findAllWhichNotAssignedToGroupType(GroupType type);

    List<AppUser> findAllByIdNotIn(Collection<Long> ids);
}
