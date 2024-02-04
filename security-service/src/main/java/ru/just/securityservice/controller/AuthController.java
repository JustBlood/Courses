package ru.just.securityservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.just.dtolib.response.ApiResponse;
import ru.just.securityservice.dto.UserDto;
import ru.just.securityservice.service.AuthService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

//    @PostMapping("/register")
//    public ResponseEntity<ApiResponse> register(UserDto userDto) {
//
//    }
}
