package ru.just.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.dtolib.response.ApiResponse;
import ru.just.userservice.dto.UpdateUserDto;
import ru.just.userservice.dto.UserDto;
import ru.just.userservice.service.UserService;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDto>> getUsersInfoByIds(@RequestParam(name = "ids") List<Long> usersInfoByIdsDto) {
        return ResponseEntity.ok(userService.getUsersByIds(usersInfoByIdsDto));
    }

    @GetMapping("/byId")
    public ResponseEntity<UserDto> getUserInfoById() {
        return ResponseEntity.of(userService.getUserByIdFromToken());
    }

    @PutMapping("/byId")
    public ResponseEntity<Void> updateUserInfo(@Valid @RequestBody UpdateUserDto userDto) {
        userService.updateUser(userDto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/photo")
    public ResponseEntity<ApiResponse> addPhoto(HttpServletRequest httpServletRequest) throws IOException {
        final String message = "Success adding photo to user";
        userService.addPhotoToUser(httpServletRequest.getInputStream());
        ApiResponse apiResponse = new ApiResponse(message);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }
}
