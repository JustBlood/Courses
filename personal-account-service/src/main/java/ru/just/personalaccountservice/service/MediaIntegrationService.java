package ru.just.personalaccountservice.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaIntegrationService {
    public void saveFile(String photoUrl, HttpServletRequest httpServletRequest) {

    }
    // todo: отправка фото в media-service
}
