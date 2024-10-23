package ru.just.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ru.just.apigateway.security.SecurityService;

@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final SecurityService securityService;

    public JwtAuthenticationFilter(SecurityService securityService) {
        super(Config.class);
        this.securityService = securityService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Получаем токен из заголовка Authorization
            String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // Проверяем наличие токена
            if (token == null || !token.startsWith("Bearer ")) {
                log.warn("token is null or not Bearer");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Валидация токена через security-service
            log.debug("received Bearer token in Authorization Header");
            return securityService.isValidToken(token.substring(7))
                    .flatMap(isValid -> {
                        if (!isValid) {
                            log.warn("Bearer token is not valid");
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }
                        // Если токен валиден, продолжаем цепочку фильтров
                        return chain.filter(exchange);
                    })
                    .onErrorResume(e -> {
                        log.error("Error while token validation", e);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });
        };
    }

    public static class Config {
        // Конфигурационные параметры
    }
}

