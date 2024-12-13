package ru.just.personalaccountservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.just.dtolib.response.media.FileIdDto;
import ru.just.dtolib.response.media.FileUrlDto;
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

    public FileUrlDto updateProfilePhoto(MultipartFile file) {
        if (!"image/png".equals(file.getContentType())) {
            throw new IllegalArgumentException("photo should be png file");
        }
        final FileIdDto fileIdDto = mediaIntegrationService.uploadAvatar(file);
        return mediaIntegrationService.getAvatar(fileIdDto.getFileId());
    }

    public Optional<UserDto> getUserData() {
        // запрос в users-service на получение данных о пользователе + кеширование 10 минут можно
        return userIntegrationService.getUserData();
    }
}
