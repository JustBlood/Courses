package ru.just.mediaservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.just.mediaservice.repository.MediaRepository;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {
    public static final String USERS_AVATAR_PATH_PATTERN = "/%d/avatar/photo.png";
    public static final String CHAT_ATTACHMENTS_PATH_PATTERN = "/%s/attachments/%s.png";
    private final MediaRepository mediaRepository;
    private final ThreadLocalTokenService tokenService;

    public String saveFile(String objectFullPathName, MultipartFile file) {
        return mediaRepository.saveUserFile(objectFullPathName, file);
    }

    public String uploadAvatarPhoto(MultipartFile file) {
        checkPngContentType(file);

        String avatarPath = String.format(USERS_AVATAR_PATH_PATTERN, tokenService.getUserId());
        return saveFile(avatarPath, file);
    }

    public Optional<String> getAvatarUrlForCurrentUser() {
        String avatarPath = String.format(USERS_AVATAR_PATH_PATTERN, tokenService.getUserId());
        if (mediaRepository.checkFileExists(avatarPath)) {
            return Optional.of(avatarPath);
        }
        return Optional.empty();
    }

    public String uploadChatAttachment(UUID chatId, MultipartFile file) {
        checkPngContentType(file);

        String fullPath = String.format(CHAT_ATTACHMENTS_PATH_PATTERN, chatId, UUID.randomUUID());
        mediaRepository.saveChatAttachment(fullPath, file);
        return fullPath;
    }

    private static void checkPngContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (!"image/png".equals(contentType)) {
            throw new IllegalArgumentException("file for uploading should be png");
        }
    }

    public String getPresignedUrlForAttachment(String fullPathToAttachment) {
        return mediaRepository.getPresignedUrlForAttachment(fullPathToAttachment);
    }

    public String generateAvatarFor(Long userId, String username) {
        var imageOutputStream = AvatarGenerator.generateAvatar(420, username);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageOutputStream.toByteArray());
        String avatarPath = String.format(USERS_AVATAR_PATH_PATTERN, userId);
        return mediaRepository.saveUserFile(avatarPath, inputStream, imageOutputStream.size());
    }
}
