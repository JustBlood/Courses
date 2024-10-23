package ru.just.mediaservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.just.mediaservice.service.MediaService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;

    @PostMapping("/upload/avatar")
    public ResponseEntity<String> uploadAvatarPhoto(@RequestParam("file") MultipartFile file) {
        String fileUrl = mediaService.uploadAvatarPhoto(file);
        return ResponseEntity.ok(fileUrl);
    }

    @PostMapping("/chats/{chatId}/attachments/upload")
    public ResponseEntity<String> uploadChatAttachment(
            @PathVariable UUID chatId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok().body(mediaService.uploadChatAttachment(chatId, file));
    }

    @GetMapping("/chats/{chatId}/attachments")
    public ResponseEntity<String> getPresignedUrlForAttachment(@RequestParam("pathTo") String fullPathToAttachment) {
        return ResponseEntity.ok(mediaService.getPresignedUrlForAttachment(fullPathToAttachment));
    }

    @GetMapping
    public ResponseEntity<String> getUserAvatarUrl() {
        return ResponseEntity.of(mediaService.getAvatarUrlForCurrentUser());
    }
}
