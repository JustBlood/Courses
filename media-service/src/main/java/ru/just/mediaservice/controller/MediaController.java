package ru.just.mediaservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping
    public ResponseEntity<String> getUserAvatarUrl() {
        return ResponseEntity.of(mediaService.getAvatarUrlForCurrentUser());
    }
}
