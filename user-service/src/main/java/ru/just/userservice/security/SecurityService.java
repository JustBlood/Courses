package ru.just.userservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class SecurityService {
    private final WebClient.Builder webClient;

    @Cacheable(value = "token-validity", key = "#token")
    public boolean isValidToken(String token) {
        return webClient.build().post()
                .uri("https://localhost:8083/api/v1/auth/token/validate?token=" + token)
                .retrieve().toBodilessEntity().block(Duration.ofSeconds(20)).getStatusCode().is2xxSuccessful();
    }
}
