package ru.just.personalaccountservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.dtolib.response.ApiResponse;
import ru.just.personalaccountservice.dto.UpdateUserDto;
import ru.just.personalaccountservice.dto.UserDto;
import ru.just.personalaccountservice.service.PersonalAccountService;

@RestController
@RequestMapping("/api/v1/user/account/data")
@RequiredArgsConstructor
public class PersonalAccountController {
    private final PersonalAccountService personalAccountService;

    @GetMapping
    public ResponseEntity<UserDto> getUserData() {
        UserDto userDto = personalAccountService.getUserData();
        return ResponseEntity.ok(userDto);
    }

    @PatchMapping
    public ResponseEntity<ApiResponse> updateUserData(@Valid @RequestBody UpdateUserDto updateUserDto) {
        personalAccountService.updateUserData(updateUserDto);
        return ResponseEntity.ok(new ApiResponse("update sent successfully"));
    }

    @PostMapping("/photo")
    public ResponseEntity<ApiResponse> updateProfilePhoto(HttpServletRequest httpServletRequest) {
        personalAccountService.updateProfilePhoto(httpServletRequest);
        return ResponseEntity.ok(new ApiResponse("user photo updated successfully"));
    }
}