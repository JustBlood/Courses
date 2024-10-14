package ru.just.personalaccountservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.just.dtolib.response.ApiResponse;
import ru.just.personalaccountservice.dto.UpdateUserDto;
import ru.just.personalaccountservice.dto.UserDto;
import ru.just.personalaccountservice.service.PersonalAccountService;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/user/account/data")
@RequiredArgsConstructor
public class PersonalAccountController {
    private final PersonalAccountService personalAccountService;

    @GetMapping
    public ResponseEntity<UserDto> getUserData() {
        Optional<UserDto> userDto = personalAccountService.getUserData();
        return ResponseEntity.of(userDto);
    }

    @PatchMapping
    public ResponseEntity<ApiResponse> updateUserData(@Valid @RequestBody UpdateUserDto updateUserDto) {
        personalAccountService.updateUserData(updateUserDto);
        return ResponseEntity.ok(new ApiResponse("update sent successfully"));
    }

    @PostMapping("/photo")
    public ResponseEntity<ApiResponse> updateProfilePhoto(@RequestParam("file") MultipartFile file) {
        String photoUrl = personalAccountService.updateProfilePhoto(file);
        return ResponseEntity.ok(new ApiResponse(photoUrl));
    }
}
