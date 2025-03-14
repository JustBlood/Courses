package ru.just.userservice.service.integration;

import ru.just.protos.mediaservice.avatargenerate.*;

public interface MediaServiceProtobuf {
    FileId generateAvatar(Username username);
    PresignedFileUrl getPresignedUrlForAttachment(FileId fileId);
    PresignedFileUrlsByUserAvatars getPresignedUrlsByUserAvatars(FileIds fileIds);
}
