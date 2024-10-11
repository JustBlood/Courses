package ru.just.mediaservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MediaService {
    public String saveFile(MultipartFile file) {
        // todo: логика работы с minio
        return null;
    }

    public void deleteFile(String fileName) {
        // todo: логика работы с minio
    }

    public String getFileUrl(String fileName) {
        // todo: логика работы с minio
        return null;
    }
}
