package ru.just.mediaservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.just.mediaservice.repository.MediaRepository;
import ru.just.securitylib.service.ThreadLocalTokenService;

@Service
@RequiredArgsConstructor
public class MediaService {
    public static final String USERS_AVATAR_PATH_PATTERN = "/%d/avatar/%s";
    private final MediaRepository mediaRepository;
    private final ThreadLocalTokenService tokenService;

    public String saveFile(String objectFullPathName, MultipartFile file) {
        mediaRepository.saveFile(objectFullPathName, file);
        return null;
    }

    public String uploadAvatarPhoto(MultipartFile file) {
        String contentType = file.getContentType();
        if (!"image/png".equals(contentType)) {
            throw new IllegalArgumentException("file for avatar uploading should be png");
        }

        String avatarPath = String.format(USERS_AVATAR_PATH_PATTERN, tokenService.getUserId(), "photo.png");
        return saveFile(avatarPath, file);
    }
}
