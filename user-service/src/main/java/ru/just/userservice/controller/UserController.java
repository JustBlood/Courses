package ru.just.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.dtolib.response.ApiResponse;
import ru.just.userservice.dto.CreateUserDto;
import ru.just.userservice.dto.UpdateUserDto;
import ru.just.userservice.dto.UserDto;
import ru.just.userservice.service.UserService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users/admin")
public class UserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long userId) {
        return ResponseEntity.of(userService.getUserById(userId));
    }

    @PostMapping
    public ResponseEntity<UserDto> saveUser(@Valid @RequestBody CreateUserDto createUserDto) {
        UserDto userDto = userService.createUser(createUserDto);
        return new ResponseEntity<>(userDto, HttpStatus.CREATED);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<Void> updateUser(@PathVariable Long userId,
                                              @Valid @RequestBody UpdateUserDto userDto) {
        userService.updateUser(userId, userDto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return new ResponseEntity<>(new ApiResponse("user was successfully deleted"), HttpStatus.OK);
    }
}
