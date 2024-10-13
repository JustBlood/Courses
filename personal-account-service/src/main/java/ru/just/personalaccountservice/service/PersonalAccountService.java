package ru.just.personalaccountservice.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.just.personalaccountservice.dto.UpdateUserDto;
import ru.just.personalaccountservice.dto.UserDto;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PersonalAccountService {
    private final UserIntegrationService userIntegrationService;
    private final MediaIntegrationService mediaIntegrationService;

    public void updateUserData(UpdateUserDto updateUserDto) {
        userIntegrationService.sendUpdateUserDataMessage(updateUserDto);
    }

    public void updateProfilePhoto(HttpServletRequest httpServletRequest) {
        String photoUrl = "url";
        mediaIntegrationService.saveFile(photoUrl, httpServletRequest);
    }

    public Optional<UserDto> getUserData() {
        // запрос в users-service на получение данных о пользователе + кеширование 10 минут можно
        return userIntegrationService.getUserData();
    }
}
