package ru.just.userservice.service.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.just.protos.mediaservice.avatargenerate.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceProtobufImpl implements MediaServiceProtobuf {
    public static final String MEDIA_SERVICE_URL = "http://media-service/api/v2/media/internal";
    private final RestTemplate restTemplate;

    @Override
    public FileId generateAvatar(Username username) {
        return restTemplate.postForEntity(MEDIA_SERVICE_URL + Path.GENERATE_AVATAR, username, FileId.class)
                .getBody();
    }

    @Override
    public PresignedFileUrl getPresignedUrlForAttachment(FileId fileId) {
        return restTemplate.postForEntity(MEDIA_SERVICE_URL + Path.GET_URL_FOR_ATTACHMENT, fileId, PresignedFileUrl.class)
                .getBody();
    }

    @Override
    public PresignedFileUrlsByUserAvatars getPresignedUrlsByUserAvatars(FileIds fileIds) {
        return restTemplate.postForEntity(MEDIA_SERVICE_URL + Path.GET_URLS_BY_USER_AVATARS, fileIds, PresignedFileUrlsByUserAvatars.class)
                .getBody();
    }

    public static class Path {
        public static final String GENERATE_AVATAR = "/avatar/generate";
        public static final String GET_URL_FOR_ATTACHMENT = "/chatAttachments";
        public static final String GET_URLS_BY_USER_AVATARS = "/avatar";
    }
}
