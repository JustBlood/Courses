package ru.just.mediaservice.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.just.dtolib.response.media.FileIdDto;
import ru.just.dtolib.response.media.FileUrlDto;
import ru.just.mediaservice.service.MediaService;
import ru.just.mediaservice.service.PresignedAvatarUrlsLoader;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media/internal")
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;
    private final PresignedAvatarUrlsLoader avatarUrlsLoader;

    @PostMapping("/avatar/generate")
    public ResponseEntity<FileIdDto> generateAvatarPhoto(@RequestParam("username") String username) {
        final UUID fileId = mediaService.generateAvatarFor(username);
        return ResponseEntity.ok(new FileIdDto(fileId));
    }

    @PostMapping("/avatar/upload")
    public ResponseEntity<FileIdDto> uploadAvatarPhoto(@RequestParam("file") MultipartFile file) {
        UUID fileUrl = mediaService.uploadAvatarPhoto(file);
        return ResponseEntity.ok(new FileIdDto(fileUrl));
    }

    @PostMapping("/chatAttachments")
    public ResponseEntity<FileIdDto> uploadChatAttachment(
            @RequestParam("file") MultipartFile file
    ) {
        final UUID fileId = mediaService.uploadChatAttachment(file);
        return ResponseEntity.ok(new FileIdDto(fileId));
    }

    @GetMapping("/chatAttachments")
    public ResponseEntity<String> getPresignedUrlForAttachment(@RequestParam("fileId") UUID fileId) {
        return ResponseEntity.ok(mediaService.getPresignedUrlForAttachment(fileId));
    }

    @GetMapping("/avatar")
    public ResponseEntity<Map<UUID, FileUrlDto>> getPresignedUrlForUsersAvatars(@RequestParam("fileIds") List<UUID> fileIds) {
        return ResponseEntity.ok(avatarUrlsLoader.getPresignedAvatarUrls(fileIds));
    }
}
