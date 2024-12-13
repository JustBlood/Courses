package ru.just.mediaservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.just.mediaservice.repository.MediaRepository;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {
    private final MediaRepository mediaRepository;

    @Value("${minio.buckets.users-data}")
    private String usersBucket;
    @Value("${minio.buckets.chat-data}")
    private String chatAttachmentsBucket;

    public UUID saveFile(String objectFullPathName, MultipartFile file) {
        return mediaRepository.saveFile(objectFullPathName, file);
    }

    public UUID uploadAvatarPhoto(MultipartFile file) {
        checkPngContentType(file);

        return saveFile(usersBucket, file);
    }

    @Cacheable(value = "presigned-url", key = "#fileId")
    public String getPresignedAvatarUrl(UUID fileId) {
        return mediaRepository.getPresignedUrlForFile(usersBucket, fileId);
    }

    public UUID uploadChatAttachment(MultipartFile file) {
        checkPngContentType(file);

        return mediaRepository.saveFile(chatAttachmentsBucket, file);
    }

    private static void checkPngContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (!"image/png".equals(contentType)) {
            throw new IllegalArgumentException("file for uploading should be png");
        }
    }

    @Cacheable(value = "presigned-url", key = "#fileId")
    public String getPresignedUrlForAttachment(UUID fileId) {
        return mediaRepository.getPresignedUrlForFile(chatAttachmentsBucket, fileId);
    }

    public UUID generateAvatarFor(String username) {
        var imageOutputStream = AvatarGenerator.generateAvatar(420, username);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageOutputStream.toByteArray());
        return mediaRepository.saveFile(usersBucket, inputStream, imageOutputStream.size());
    }
}
