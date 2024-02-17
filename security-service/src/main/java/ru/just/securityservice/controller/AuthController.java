package ru.just.securityservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.just.dtolib.response.ApiResponse;
import ru.just.securityservice.dto.CreateUserDto;
import ru.just.securityservice.service.UserService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<String> getGreeting(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("Hello, %s!".formatted(user.getUsername()));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody CreateUserDto createUserDto) {
        userService.register(createUserDto);
        return new ResponseEntity<>(new ApiResponse("User registered successfully"), HttpStatus.CREATED);
    }
}
