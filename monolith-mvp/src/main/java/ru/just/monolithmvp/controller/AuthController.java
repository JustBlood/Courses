package ru.just.monolithmvp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.just.monolithmvp.dto.ApiResponse;
import ru.just.monolithmvp.dto.auth.ChangePasswordRequest;
import ru.just.monolithmvp.dto.auth.LoginRequest;
import ru.just.monolithmvp.dto.auth.LoginResponse;
import ru.just.monolithmvp.dto.auth.SetPasswordRequest;
import ru.just.monolithmvp.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse> setPassword(@RequestParam String token,
                                                   @Valid @RequestBody SetPasswordRequest request) {
        authService.setPassword(token, request);
        return ResponseEntity.ok(new ApiResponse("Password has been set"));
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(new ApiResponse("Password has been changed"));
    }
}
