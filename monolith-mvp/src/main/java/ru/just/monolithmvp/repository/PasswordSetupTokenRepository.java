package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.just.monolithmvp.model.PasswordSetupToken;

import java.util.Optional;

public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupToken, Long> {
    Optional<PasswordSetupToken> findByToken(String token);
    void deleteByUser_Id(Long userId);

    @Query("select t from PasswordSetupToken t join fetch t.user where t.token = :token")
    Optional<PasswordSetupToken> findByTokenWithUser(@Param("token") String token);
}
