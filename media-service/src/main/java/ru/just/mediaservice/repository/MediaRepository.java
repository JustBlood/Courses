package ru.just.mediaservice.repository;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MediaRepository {
    private final MinioClient minioClient;

    @Value("${minio.buckets.users-data}")
    private String usersBucket;
    @Value("${minio.buckets.chat-data}")
    private String chatAttachmentsBucket;

    public String saveUserFile(String objectFullPathName, MultipartFile file) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(usersBucket)
                            .object(objectFullPathName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build()
            );
            return "/" + usersBucket + objectFullPathName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkFileExists(String avatarPath) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(usersBucket)
                            .object(avatarPath)
                            .build()
            );
            return true;
        } catch (Exception e) {
            log.debug("Object with path {} not found", avatarPath, e);
            return false;
        }
    }

    public void saveChatAttachment(String objectFullPathName, MultipartFile file) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(chatAttachmentsBucket)
                            .object(objectFullPathName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getPresignedUrlForAttachment(String fullPathToFile) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(chatAttachmentsBucket)
                            .object(fullPathToFile)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
