package ru.just.mediaservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.just.mediaservice.service.MediaService;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController { // todo: настроить minio, чтобы был доступ открытый ко всем аватаркам
    private final MediaService mediaService;

    @PostMapping("/upload/avatar")
    public ResponseEntity<String> uploadAvatarPhoto(@RequestParam("file") MultipartFile file) {
        String fileUrl = mediaService.uploadAvatarPhoto(file);
        return ResponseEntity.ok(fileUrl);
    }
}
