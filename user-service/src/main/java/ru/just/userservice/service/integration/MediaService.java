package ru.just.userservice.service.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import ru.just.dtolib.response.media.FileIdDto;
import ru.just.dtolib.response.media.FileUrlDto;
import ru.just.userservice.config.FeignConfiguration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "media-service", path = "/api/v1/media/internal", configuration = FeignConfiguration.class)
public interface MediaService {
    @PostMapping("/avatar/generate")
    FileIdDto generateAvatar(@RequestParam("username") String username);

    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileIdDto uploadAvatarPhoto(@RequestPart(value = "file") MultipartFile file);

    @GetMapping(value = "/avatar")
    Map<UUID, FileUrlDto> getAvatar(@RequestParam("fileIds") List<UUID> fileIds);
}
