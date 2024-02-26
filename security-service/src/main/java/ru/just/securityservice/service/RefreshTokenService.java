package ru.just.securityservice.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.securityservice.model.RefreshToken;
import ru.just.securityservice.model.User;
import ru.just.securityservice.repository.RefreshTokenRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public boolean isRefreshTokenValid(UUID id, UUID deviceId, long userId) {
        return refreshTokenRepository.existsByIdAndExpiresAtAfterAndDeviceIdAndUser_UserId(id, Instant.now(), deviceId, userId);
    }

    public void saveIssuedRefreshToken(Long userId, DecodedJWT refreshToken, UUID deviceId) {
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .id(UUID.fromString(refreshToken.getId()))
                .user(new User().setUserId(userId))
                .createdAt(refreshToken.getIssuedAtAsInstant())
                .expiresAt(refreshToken.getExpiresAtAsInstant())
                .deviceId(deviceId)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);
    }

    @Transactional
    public void deleteAllTokensByUserId(Long userId) {
        refreshTokenRepository.deleteAllByUser_UserId(userId);
    }

    public void deleteById(UUID id) {
        refreshTokenRepository.deleteById(id);
    }

    @Transactional
    public void deleteTokenByUserIdAndDeviceId(long userId, UUID deviceId) {
        refreshTokenRepository.deleteByUser_UserIdAndDeviceId(userId, deviceId);
    }

    public Optional<RefreshToken> findById(UUID tokenId) {
        return refreshTokenRepository.findById(tokenId);
    }
}
