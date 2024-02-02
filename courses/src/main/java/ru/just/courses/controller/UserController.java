package ru.just.courses.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.courses.controller.response.ApiResponse;
import ru.just.courses.dto.CreateUserDto;
import ru.just.courses.dto.UserDto;
import ru.just.courses.service.UserService;

// TODO: выделить в отдельный микросервис
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long userId) {
        return ResponseEntity.of(userService.getUserDtoById(userId));
    }

    @PostMapping
    public ResponseEntity<UserDto> saveUser(@Valid @RequestBody CreateUserDto createUserDto) {
        UserDto userDto = userService.saveUser(createUserDto);
        return new ResponseEntity<>(userDto, HttpStatus.CREATED);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return new ResponseEntity<>(new ApiResponse("user was successfully deleted"), HttpStatus.OK);
    }
}
