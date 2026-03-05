package ru.just.monolithmvp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.just.monolithmvp.exception.BadRequestException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.storage.root-dir:data}")
    private String rootDir;

    public String store(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String safeDirectory = sanitizeDirectory(directory);
        String extension = extractExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + extension;

        try {
            Path rootPath = resolveRootPath();
            Path dirPath = rootPath.resolve(safeDirectory).normalize();

            Files.createDirectories(dirPath);

            Path target = dirPath.resolve(fileName).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = rootPath.relativize(target).toString().replace('\\', '/');
            return "/files/" + relativePath;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", ex);
        }
    }

    public void deleteIfExists(String path) {
        if (path == null || path.isBlank()) {
            return;
        }

        try {
            String normalized = normalizeStoredPath(path);
            if (normalized == null || !normalized.startsWith("/files/")) {
                return;
            }

            String relativePath = normalized.substring("/files/".length());
            Path filePath = resolveRootPath().resolve(relativePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.error("IOException in file deleting.", ex);
        }
    }

    public String normalizeStoredPath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        String path = storedPath.trim().replace('\\', '/');
        if (path.startsWith("/files/")) {
            return path;
        }
        if (path.startsWith("files/")) {
            return "/" + path;
        }
        return "/files/" + (path.startsWith("/") ? path.substring(1) : path);
    }

    private Path resolveRootPath() {
        return Path.of(rootDir).toAbsolutePath().normalize();
    }

    private String sanitizeDirectory(String directory) {
        if (directory == null || directory.isBlank()) {
            return "uploads";
        }

        String normalized = directory.replace('\\', '/').trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (normalized.isBlank() || normalized.contains("..")) {
            throw new BadRequestException("Invalid directory");
        }

        return normalized;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return ".bin";
        }

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return ".bin";
        }

        return originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }
}
