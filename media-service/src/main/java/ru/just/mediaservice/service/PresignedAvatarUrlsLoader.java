package ru.just.mediaservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.just.dtolib.response.media.FileUrlDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class PresignedAvatarUrlsLoader {
    private final MediaService mediaService;


    public Map<UUID, FileUrlDto> getPresignedAvatarUrls(List<UUID> fileIds) {
        return fileIds.stream()
                .collect(toMap(
                        fileId -> fileId,
                        fileId -> new FileUrlDto(mediaService.getPresignedAvatarUrl(fileId))
                ));
    }
}
