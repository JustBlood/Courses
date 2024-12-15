package ru.just.communicationservice.service.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import ru.just.dtolib.response.media.FileIdDto;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaIntegrationService {
    private final RestTemplate restTemplate;

    @Value("http://${service-discovery.media-service.name}/api/v1/media/internal")
    private String mediaServiceUri;

    public FileIdDto saveChatAttachmentPhoto(MultipartFile file) {
        if (!"image/png".equals(file.getContentType())) {
            throw new IllegalArgumentException("photo should be png file");
        }
        final String uriTemplate = mediaServiceUri + "/chatAttachments";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        RequestEntity<MultiValueMap<String, Object>> saveAttachmentRequest = RequestEntity
                .post(uriTemplate)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body);
        try {
            ResponseEntity<FileIdDto> response = restTemplate.exchange(saveAttachmentRequest, FileIdDto.class);
            return response.getBody();
        } catch (HttpClientErrorException clientException) {
            log.error("Error in post photo request", clientException);
            throw clientException;
        } catch (HttpServerErrorException serverException) {
            log.error("Media service can't handle request {}", saveAttachmentRequest, serverException);
            throw serverException;
        } catch (RestClientException e) {
            log.error("Unexpected error in post photo to media-service", e);
            throw e;
        }
    }

    public String getPresignedUrlForAttachment(UUID fileId) {
        final String uriTemplate = mediaServiceUri + "/chatAttachments?fileId={fileId}";
        RequestEntity<Void> getPresignedUrlRequest = RequestEntity
                .get(uriTemplate, fileId)
                .build();
        try {
            ResponseEntity<String> response = restTemplate.exchange(getPresignedUrlRequest, String.class);
            return response.getBody();
        } catch (HttpClientErrorException clientException) {
            log.error("Error in post photo request", clientException);
            throw clientException;
        } catch (HttpServerErrorException serverException) {
            log.error("Media service can't handle request {}", getPresignedUrlRequest, serverException);
            throw serverException;
        } catch (RestClientException e) {
            log.error("Unexpected error in post photo to media-service", e);
            throw e;
        }
    }
}
