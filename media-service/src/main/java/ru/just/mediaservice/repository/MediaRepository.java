package ru.just.mediaservice.repository;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import ru.just.mediaservice.config.MinioProperties;

import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MediaRepository {
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public UUID saveFile(String bucket, MultipartFile file) {
        try {
            return saveFile(bucket, file.getInputStream(), file.getSize(), file.getContentType(), file.getOriginalFilename());
        } catch (Exception e) {
            log.error("Can't get InputStream for file: ", e);
            throw new RuntimeException(e);
        }
    }

    public UUID saveFile(String bucket, InputStream is, long size, String contentType, String originalFileName) {
        try {
            UUID fileId = UUID.randomUUID();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileId + "/" + originalFileName)
                            .contentType(contentType)
                            .stream(is, size, -1)
                            .build()
            );
            return fileId;
        } catch (Exception e) {
            log.error("Error while saving file", e);
            throw new RuntimeException(e);
        }
    }

    public boolean checkFileExists(String bucket, UUID fileId) {
        try {
            final List<Result<Item>> findObject = Lists.newArrayList(minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .prefix(fileId + "/")
                            .build()
            ).iterator());
            if (findObject.size() > 1) {
                throw new IllegalArgumentException("Find more than one object with fileId: " + fileId);
            }
            return findObject.size() == 1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getPresignedUrlForFile(String bucket, UUID fileId) {
        try {
            final List<Result<Item>> findObject = Lists.newArrayList(minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .prefix(fileId + "/")
                            .build()
            ).iterator());

            if (findObject.size() > 1) {
                throw new IllegalArgumentException("Find more than one object with fileId: " + fileId);
            }
            if (findObject.isEmpty()) {
                throw new NoSuchElementException(format("File with id %s not found", fileId));
            }

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(findObject.getFirst().get().objectName())
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
