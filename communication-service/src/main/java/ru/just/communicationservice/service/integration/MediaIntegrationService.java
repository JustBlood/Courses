package ru.just.communicationservice.service.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaIntegrationService {
    public static final String BEARER_PREFIX = "Bearer ";
    private final RestTemplate restTemplate;
    private final ThreadLocalTokenService tokenService;

    @Value("http://${service-discovery.media-service.name}/api/v1/media")
    private String mediaServiceUri;

    public String saveChatAttachmentPhoto(UUID chatId, MultipartFile file) {
        if (!"image/png".equals(file.getContentType())) {
            throw new IllegalArgumentException("photo should be png file");
        }
        final String uriTemplate = mediaServiceUri + "/chats/{chatId}/attachments/upload";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        RequestEntity<MultiValueMap<String, Object>> saveAttachmentRequest = RequestEntity
                .post(uriTemplate, chatId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .headers(buildHeaders())
                .body(body);
        try {
            ResponseEntity<String> response = restTemplate.exchange(saveAttachmentRequest, String.class);
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

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + tokenService.getDecodedToken().getToken());
        return headers;
    }

    public String getPresignedUrlForAttachment(UUID chatId, String fullPathToAttachment) {
        final String uriTemplate = mediaServiceUri + "/chats/{chatId}/attachments?pathTo={pathTo}";
        RequestEntity<Void> getPresignedUrlRequest = RequestEntity
                .get(uriTemplate, chatId, fullPathToAttachment)
                .headers(buildHeaders())
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
