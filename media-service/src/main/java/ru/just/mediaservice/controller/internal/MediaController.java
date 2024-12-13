package ru.just.mediaservice.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.dtolib.response.media.FileIdDto;
import ru.just.mediaservice.service.MediaService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media/internal")
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;

    @PostMapping("/generate/avatar")
    public ResponseEntity<FileIdDto> generateAvatarPhoto(@RequestParam("username") String username) {
        final UUID fileId = mediaService.generateAvatarFor(username);
        return ResponseEntity.ok(new FileIdDto(fileId));
    }
}
