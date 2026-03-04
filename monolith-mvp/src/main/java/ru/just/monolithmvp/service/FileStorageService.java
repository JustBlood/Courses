package ru.just.monolithmvp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.just.monolithmvp.exception.BadRequestException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.storage.root-dir:data}")
    private String rootDir;

    @Value("${app.storage.public-base-url:http://localhost:8099/files}")
    private String publicBaseUrl;

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

            return target.toString().replace('\\', '/');
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", ex);
        }
    }

    public String toPublicUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored link is empty");
        }

        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Storage public base URL is not configured");
        }

        try {
            Path rootPath = resolveRootPath();
            Path filePath = Path.of(storedPath).toAbsolutePath().normalize();
            if (!filePath.startsWith(rootPath)) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored file is outside storage root");
            }

            String relativePath = rootPath.relativize(filePath).toString().replace('\\', '/');
            String normalizedBaseUrl = publicBaseUrl.endsWith("/")
                    ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                    : publicBaseUrl;
            return normalizedBaseUrl + "/" + relativePath;
        } catch (InvalidPathException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid stored file link", ex);
        }
    }

    public void deleteIfExists(String path) {
        if (path == null || path.isBlank()) {
            return;
        }

        if (path.startsWith("http://") || path.startsWith("https://")) {
            return;
        }

        try {
            Files.deleteIfExists(Path.of(path));
        } catch (IOException | InvalidPathException ignored) {
            // no-op for MVP
        }
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
