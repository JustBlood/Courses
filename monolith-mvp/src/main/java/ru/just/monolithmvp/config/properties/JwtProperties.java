package ru.just.monolithmvp.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        Long expirationMs
) {
}
