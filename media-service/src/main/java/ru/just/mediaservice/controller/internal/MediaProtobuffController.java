package ru.just.mediaservice.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.just.mediaservice.service.MediaService;
import ru.just.mediaservice.service.PresignedAvatarUrlsLoader;
import ru.just.protos.mediaservice.avatargenerate.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toMap;

@RestController
@RequestMapping("/api/v2/media/internal")
@RequiredArgsConstructor
public class MediaProtobuffController {
    private final MediaService mediaService;
    private final PresignedAvatarUrlsLoader avatarUrlsLoader;

    @PostMapping(path = "/avatar/generate", consumes = "application/x-protobuf", produces = "application/x-protobuf")
    public ResponseEntity<FileId> generateAvatarPhoto(@RequestBody Username username) {
        final UUID fileId = mediaService.generateAvatarFor(username.getName());
        return ResponseEntity.ok(FileId.newBuilder().setFileUUID(fileId.toString()).build());
    }

    @PostMapping(path = "/chatAttachments", consumes = "application/x-protobuf", produces = "application/x-protobuf")
    public ResponseEntity<PresignedFileUrl> getPresignedUrlForAttachment(@RequestBody FileId fileId) {
        return ResponseEntity.ok(PresignedFileUrl.newBuilder()
                .setUrl(mediaService.getPresignedUrlForAttachment(UUID.fromString(fileId.getFileUUID())))
                .build()
        );
    }

    @PostMapping(path = "/avatar", consumes = "application/x-protobuf", produces = "application/x-protobuf")
    public ResponseEntity<PresignedFileUrlsByUserAvatars> getPresignedUrlForUsersAvatars(@RequestBody FileIds fileIds) {
        final List<UUID> fileIdsList = fileIds.getFileIdsList().stream().map(fileId -> UUID.fromString(fileId.getFileUUID())).toList();
        final Map<String, PresignedFileUrl> presignedAvatarUrls = avatarUrlsLoader.getPresignedAvatarUrls(fileIdsList)
                .entrySet().stream()
                .collect(toMap(
                        entry -> entry.getKey().toString(),
                        entry -> PresignedFileUrl.newBuilder().setUrl(entry.getValue().getUrl()).build()
                ));
        return ResponseEntity.ok(
                PresignedFileUrlsByUserAvatars.newBuilder()
                        .putAllUrlsByFileUUID(presignedAvatarUrls)
                        .build()
        );
    }
}
