package ru.just.userservice.service.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import ru.just.dtolib.response.media.FileIdDto;
import ru.just.userservice.config.FeignConfiguration;

import java.util.UUID;

@FeignClient(name = "media-service", path = "/api/v1/media/internal", configuration = FeignConfiguration.class)
public interface MediaService {
    @PostMapping("/avatar/generate")
    UUID generateAvatar(@RequestParam("username") String username);

    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileIdDto uploadAvatarPhoto(@RequestPart(value = "file") MultipartFile file);
}
