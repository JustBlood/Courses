package ru.just.personalaccountservice.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.just.personalaccountservice.dto.UpdateUserDto;
import ru.just.personalaccountservice.dto.UserDto;

@Service
@RequiredArgsConstructor
public class UserIntegrationService {
    private final RestTemplate restTemplate;

    public void sendUpdateUserDataMessage(UpdateUserDto updateUserDto) {
        // todo: работа с кафкой - отсылать сообщения
        // todo: работа с токенами - в них id пользователя
    }

    public void updateProfilePhoto(HttpServletRequest httpServletRequest) {
        // todo: user_id из токена, название аватарки одинаковое, namespace user-a будет известен на users-service
        // todo: синхронный запрос в users-service
    }

    public UserDto getUserData(Long userId) {
        // todo: взять данные о пользователе по id
        return null;
    }
}
