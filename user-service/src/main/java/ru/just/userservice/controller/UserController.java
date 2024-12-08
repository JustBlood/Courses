package ru.just.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.just.dtolib.response.ApiResponse;
import ru.just.userservice.dto.UpdateUserDto;
import ru.just.userservice.dto.UserDto;
import ru.just.userservice.service.UserService;

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

    @PostMapping(value = "/photo")
    public ResponseEntity<ApiResponse> addPhoto(@RequestParam("avatar") MultipartFile avatar) {
        final String message = "Success adding photo to user";
        userService.addPhotoToUser(avatar);
        ApiResponse apiResponse = new ApiResponse(message);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }
}
