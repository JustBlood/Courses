package ru.just.securityservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.just.dtolib.response.ApiResponse;
import ru.just.securityservice.dto.CreateUserDto;
import ru.just.securityservice.service.AuthService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(CreateUserDto createUserDto) {
        authService.register(createUserDto);
        return new ResponseEntity<>(new ApiResponse("User registered successfully"), HttpStatus.CREATED);
    }
}
