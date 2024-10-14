package ru.just.mediaservice.repository;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MediaRepository {
    private final MinioClient minioClient;

    @Value("${minio.buckets.users-data}")
    private String usersBucket;

    public void saveFile(String objectFullPathName, MultipartFile file) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(usersBucket)
                            .object(objectFullPathName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build()
            );
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
}