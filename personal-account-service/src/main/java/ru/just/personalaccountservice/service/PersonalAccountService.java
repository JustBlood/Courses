package ru.just.personalaccountservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.just.personalaccountservice.dto.UpdateUserDto;
import ru.just.personalaccountservice.dto.UserDto;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PersonalAccountService {
    private final UserIntegrationService userIntegrationService;
    private final MediaIntegrationService mediaIntegrationService;
    private final ThreadLocalTokenService tokenService;

    public void updateUserData(UpdateUserDto updateUserDto) {
        userIntegrationService.sendUpdateUserDataMessage(updateUserDto);
    }

    public String updateProfilePhoto(MultipartFile file) {
        if (!"image/png".equals(file.getContentType())) {
            throw new IllegalArgumentException("photo should be png file");
        }
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.getDecodedToken().getToken());
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);
        return mediaIntegrationService.uploadAvatarPhoto(headers, file);
    }

    public Optional<UserDto> getUserData() {
        // запрос в users-service на получение данных о пользователе + кеширование 10 минут можно
        return userIntegrationService.getUserData();
    }
}
