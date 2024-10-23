package ru.just.apigateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {
    @LoadBalanced
    private final WebClient.Builder webClient;

    @Value("${service-discovery.security-service.name}")
    public String securityServiceName;

//    @Cacheable(value = "token-validity", key = "#token")
    public Mono<Boolean> isValidToken(String token) {
        log.debug("Sending validate token request to security service:");
        return webClient.build().post()
                .uri(uriBuilder -> uriBuilder
                        .host(securityServiceName)
                        .path("/api/v1/auth/token/validate")
                        .queryParam("token", token)
                        .build())
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .doOnError(e -> log.error("Произошла ошибка при обращении к securityService: {}", e.getMessage(), e)) // Логируем ошибку
                .onErrorReturn(false);
    }
}
