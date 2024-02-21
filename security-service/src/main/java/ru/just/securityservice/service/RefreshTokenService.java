package ru.just.securityservice.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.securityservice.model.RefreshToken;
import ru.just.securityservice.repository.RefreshTokenRepository;
import ru.just.securityservice.repository.UserRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public boolean isRefreshTokenValid(UUID id, UUID deviceId, long userId) {
        return refreshTokenRepository.existsByIdAndExpiresAtAfterAndDeviceIdAndUser_UserId(id, Instant.now(), deviceId, userId);
    }

    public void saveIssuedRefreshToken(User user, DecodedJWT refreshToken) {
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .id(UUID.fromString(refreshToken.getId()))
                .user(userRepository.findByUsername(user.getUsername()).orElseThrow()) // todo: exception handling
                .createdAt(refreshToken.getIssuedAtAsInstant())
                .expiresAt(refreshToken.getExpiresAtAsInstant())
                .deviceId(UUID.fromString(refreshToken.getClaim(SecurityService.DEVICE_ID_CLAIM).asString()))
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
}
