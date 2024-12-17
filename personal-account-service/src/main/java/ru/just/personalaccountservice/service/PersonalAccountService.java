package ru.just.personalaccountservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.just.dtolib.response.ApiResponse;
import ru.just.personalaccountservice.dto.UpdateUserDto;
import ru.just.personalaccountservice.dto.UserDto;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PersonalAccountService {
    private final UserIntegrationService userIntegrationService;
    private final ThreadLocalTokenService tokenService;

    public void updateUserData(UpdateUserDto updateUserDto) {
        userIntegrationService.sendUpdateUserDataMessage(updateUserDto);
    }

    public ApiResponse updateProfilePhoto(MultipartFile file) {
        return userIntegrationService.addAvatar(tokenService.getUserId(), file);
    }

    public Optional<UserDto> getUserData() {
        // запрос в users-service на получение данных о пользователе + кеширование 10 минут можно
        return userIntegrationService.getUserData();
    }
}
