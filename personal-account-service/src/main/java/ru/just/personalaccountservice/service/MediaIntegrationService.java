package ru.just.personalaccountservice.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import ru.just.dtolib.response.media.FileIdDto;
import ru.just.dtolib.response.media.FileUrlDto;
import ru.just.personalaccountservice.config.FeignConfiguration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "media-service", path = "/api/v1/media/internal", configuration = FeignConfiguration.class)
public interface MediaIntegrationService {
    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileIdDto uploadAvatar(@RequestPart(value = "file") MultipartFile file);

    @GetMapping(value = "/avatar")
    Map<UUID, FileUrlDto> getAvatar(@RequestParam("fileIds") List<UUID> fileId);
}
