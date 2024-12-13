package ru.just.mediaservice.repository;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MediaRepository {
    private final MinioClient minioClient;

    public UUID saveFile(String bucket, MultipartFile file) {
        try {
            return saveFile(bucket, file.getInputStream(), file.getSize());
        } catch (Exception e) {
            log.error("Can't get InputStream for file: ", e);
            throw new RuntimeException(e);
        }
    }

    public UUID saveFile(String bucket, InputStream is, long size) {
        try {
            UUID fileId = UUID.randomUUID();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileId.toString())
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
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileId.toString())
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                log.warn("File with id {} not found", fileId, e);
                return false;
            } else {
                throw new RuntimeException(e); // Обработка других ошибок
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getPresignedUrlForFile(String bucket, UUID fileId) {
        try {
            if (!checkFileExists(bucket, fileId)) {
                throw new NoSuchElementException(format("File with id %s not found", fileId));
            }
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(fileId.toString())
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
