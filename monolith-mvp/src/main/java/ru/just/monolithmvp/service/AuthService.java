package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.auth.LoginRequest;
import ru.just.monolithmvp.dto.auth.LoginResponse;
import ru.just.monolithmvp.dto.auth.SetPasswordRequest;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.model.PasswordSetupToken;
import ru.just.monolithmvp.repository.PasswordSetupTokenRepository;
import ru.just.monolithmvp.security.AuthenticatedUser;
import ru.just.monolithmvp.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordSetupTokenRepository passwordSetupTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        final String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
    }

    @Transactional
    public void setPassword(String token, SetPasswordRequest request) {
        PasswordSetupToken setupToken = passwordSetupTokenRepository.findByTokenWithUser(token)
                .orElseThrow(() -> new NotFoundException("Invalid token"));

        if (setupToken.getUsedAt() != null) {
            throw new BadRequestException("Token already used");
        }

        setupToken.getUser().setPasswordHash(passwordEncoder.encode(request.password()));
        setupToken.setUsedAt(java.time.LocalDateTime.now());
        passwordSetupTokenRepository.save(setupToken);
    }
}
