package ru.just.personalaccountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import ru.just.securitylib.service.ThreadLocalTokenService;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaIntegrationService {
    private final RestTemplate restTemplate;
    private final ThreadLocalTokenService tokenService;

    @Value("http://${service-discovery.media-service.name}/api/v1/media")
    private String mediaServiceUri;

    public String saveUserPhoto(MultipartFile file) {
        if (!"image/png".equals(file.getContentType())) {
            throw new IllegalArgumentException("photo should be png file");
        }
        final String uriTemplate = mediaServiceUri + "/upload/avatar";
        RequestEntity<MultipartFile> saveFileRequest = RequestEntity
                .post(uriTemplate)
                .contentType(MediaType.valueOf(file.getContentType()))
                .headers(buildHeaders())
                .body(file);
        try {
            ResponseEntity<String> response = restTemplate.exchange(saveFileRequest, String.class);
            return response.getBody();
        } catch (HttpClientErrorException clientException) {
            log.error("Error in post photo request", clientException);
            throw clientException;
        } catch (HttpServerErrorException serverException) {
            log.error("Media service can't handle request {}", saveFileRequest, serverException);
            throw serverException;
        } catch (RestClientException e) {
            log.error("Unexpected error in post photo to media-service", e);
            throw e;
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, tokenService.getDecodedToken().getToken());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
