package ru.just.securityservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.just.dtolib.jwt.Tokens;
import ru.just.dtolib.response.ApiResponse;
import ru.just.securityservice.dto.CreateUserDto;
import ru.just.securityservice.dto.UserDto;
import ru.just.securityservice.service.AuthService;
import ru.just.securityservice.service.UserService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<String> getGreeting(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("Hello, %s!".formatted(user.getUsername()));
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody CreateUserDto createUserDto) {
        return new ResponseEntity<>(userService.register(createUserDto), HttpStatus.CREATED);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(Authentication authentication) {
        authService.logout(authentication);
        return new ResponseEntity<>(new ApiResponse("logout successfully"), HttpStatus.NO_CONTENT);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Tokens> jwtRefresh(Authentication authentication) {
        return new ResponseEntity<>(authService.refresh(authentication), HttpStatus.OK);
    }
}