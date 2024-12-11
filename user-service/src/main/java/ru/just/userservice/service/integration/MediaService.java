package ru.just.userservice.service.integration;

import feign.HeaderMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import ru.just.userservice.config.FeignConfiguration;

import java.util.Map;

@FeignClient(name = "media-service", path = "/api/v1/media", configuration = FeignConfiguration.class)
public interface MediaService {
    @PostMapping("/internal/generate/avatar/{userId}")
    String generateAvatar(@PathVariable Long userId, @RequestParam("username") String username);

    @PostMapping(value = "/upload/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String uploadAvatarPhoto(@HeaderMap Map<String, String> headers, @RequestPart(value = "file") MultipartFile file);
}
