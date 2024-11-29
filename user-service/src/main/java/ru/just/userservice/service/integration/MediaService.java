package ru.just.userservice.service.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "media-service", path = "/api/v1/media")
public interface MediaService {
    @PostMapping("/internal/generate/avatar/{userId}")
    String generateAvatar(@PathVariable Long userId, @RequestParam("username") String username);
}
