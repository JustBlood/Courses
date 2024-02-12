package ru.just.securityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.securityservice.model.RefreshToken;

import java.time.Instant;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Boolean existsByIdAndExpiresAtAfterAndDeviceIdAndUser_UserId(UUID tokenId, Instant now, UUID deviceId, Long userId);
    void deleteByUserUserId(Long userId);
}
