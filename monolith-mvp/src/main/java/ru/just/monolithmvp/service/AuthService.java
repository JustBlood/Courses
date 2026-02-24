package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.auth.ChangePasswordRequest;
import ru.just.monolithmvp.dto.auth.LoginRequest;
import ru.just.monolithmvp.dto.auth.LoginResponse;
import ru.just.monolithmvp.dto.auth.SetPasswordRequest;
import ru.just.monolithmvp.config.properties.PasswordResetProperties;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.AppUser;
import ru.just.monolithmvp.model.PasswordSetupToken;
import ru.just.monolithmvp.observability.BusinessEventLogger;
import ru.just.monolithmvp.observability.ObservabilityMetricsService;
import ru.just.monolithmvp.repository.AppUserRepository;
import ru.just.monolithmvp.repository.PasswordSetupTokenRepository;
import ru.just.monolithmvp.security.AuthenticatedUser;
import ru.just.monolithmvp.security.JwtService;
import ru.just.monolithmvp.security.SecurityUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final PasswordSetupTokenRepository passwordSetupTokenRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final PasswordResetProperties passwordResetProperties;
    private final ObservabilityMetricsService metricsService;
    private final BusinessEventLogger businessEventLogger;

    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
            final String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole());

            metricsService.incrementAuth("login", "success");
            businessEventLogger.log("auth.login", "success", "userId", user.getId(), "email", request.email());
            return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
        } catch (RuntimeException ex) {
            metricsService.incrementAuth("login", "failure");
            businessEventLogger.log("auth.login", "failure", "email", request.email(), "reason", ex.getClass().getSimpleName());
            throw ex;
        }
    }

    @Transactional
    public void setPassword(String token, SetPasswordRequest request) {
        try {
            PasswordSetupToken setupToken = passwordSetupTokenRepository.findByTokenWithUser(token)
                    .orElseThrow(() -> new NotFoundException("Invalid token"));

            if (setupToken.getUsedAt() != null) {
                throw new BadRequestException("Token already used");
            }

            LocalDateTime expiresAt = setupToken.getCreatedAt().plus(passwordResetProperties.tokenTtl());
            if (LocalDateTime.now().isAfter(expiresAt)) {
                throw new BadRequestException("Invalid token");
            }

            setupToken.getUser().setPasswordHash(passwordEncoder.encode(request.password()));
            setupToken.getUser().setActivated(true);
            setupToken.setUsedAt(java.time.LocalDateTime.now());
            passwordSetupTokenRepository.save(setupToken);

            metricsService.incrementAuth("set_password", "success");
            businessEventLogger.log("auth.set_password", "success", "userId", setupToken.getUser().getId());
        } catch (RuntimeException ex) {
            metricsService.incrementAuth("set_password", "failure");
            businessEventLogger.log("auth.set_password", "failure", "reason", ex.getClass().getSimpleName());
            throw ex;
        }
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = securityUtils.currentUserId();
        try {
            AppUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found: " + userId));

            if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new BadRequestException("Current password is invalid");
            }

            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            userRepository.save(user);

            metricsService.incrementAuth("change_password", "success");
            businessEventLogger.log("auth.change_password", "success", "userId", userId);
        } catch (RuntimeException ex) {
            metricsService.incrementAuth("change_password", "failure");
            businessEventLogger.log("auth.change_password", "failure", "userId", userId, "reason", ex.getClass().getSimpleName());
            throw ex;
        }
    }

    public void recoverPassword(String email) {
        try {
            final AppUser user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                userService.sendPasswordLink(user);
            }

            metricsService.incrementAuth("recover_password", "success");
            businessEventLogger.log("auth.recover_password", "success", "email", email, "userFound", user != null);
        } catch (RuntimeException ex) {
            metricsService.incrementAuth("recover_password", "failure");
            businessEventLogger.log("auth.recover_password", "failure", "email", email, "reason", ex.getClass().getSimpleName());
            throw ex;
        }
    }
}
