package ru.just.mediaservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.just.mediaservice.service.MediaService;

@RestController
@RequestMapping("/api/v1/media/")
@RequiredArgsConstructor
public class MediaController { // todo: настроить minio, чтобы был доступ открытый ко всем аватаркам
    private final MediaService mediaService;

    @GetMapping("/file/{fileName}")
    public ResponseEntity<String> getFileUrl(@PathVariable String fileName) {
        String fileUrl = mediaService.getFileUrl(fileName);
        return ResponseEntity.ok(fileUrl);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = mediaService.saveFile(file);
        return ResponseEntity.ok(fileUrl);
    }

    @DeleteMapping("/delete/{fileName}")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileName) {
        mediaService.deleteFile(fileName);
        return ResponseEntity.ok().build();
    }
}
