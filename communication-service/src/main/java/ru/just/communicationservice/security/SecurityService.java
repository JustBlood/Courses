package ru.just.communicationservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {
    private final RestTemplate restTemplate;

    @Value("http://${service-discovery.security-service.name}/api/v1/auth/token/validate")
    public String securityServiceUrl;

    @Cacheable(value = "token-validity", key = "#token")
    public boolean isValidToken(String token) {
        RequestEntity<Void> requestEntity = RequestEntity
                .post(securityServiceUrl + "?token=" + token)
                .build();
        log.debug("Sending validate token request to security service:");
        return restTemplate.exchange(requestEntity, String.class).getStatusCode().is2xxSuccessful();
    }
}
