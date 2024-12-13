package ru.just.personalaccountservice.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.just.dtolib.response.media.FileIdDto;
import ru.just.dtolib.response.media.FileUrlDto;
import ru.just.personalaccountservice.config.FeignConfiguration;

import java.util.UUID;

@FeignClient(name = "media-service", path = "/api/v1/media/internal", configuration = FeignConfiguration.class)
public interface MediaIntegrationService {
    @PostMapping(value = "/upload/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileIdDto uploadAvatar(@RequestPart(value = "file") MultipartFile file);

    @GetMapping(value = "/avatar")
    FileUrlDto getAvatar(@RequestParam("fileId") UUID fileId);
}
