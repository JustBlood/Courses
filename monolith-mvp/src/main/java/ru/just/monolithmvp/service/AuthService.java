package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import ru.just.monolithmvp.dto.auth.LoginRequest;
import ru.just.monolithmvp.dto.auth.LoginResponse;
import ru.just.monolithmvp.security.AuthenticatedUser;
import ru.just.monolithmvp.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        final String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
    }
}
