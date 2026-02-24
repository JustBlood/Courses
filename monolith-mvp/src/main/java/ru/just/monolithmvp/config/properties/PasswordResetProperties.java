package ru.just.monolithmvp.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.password-reset")
public record PasswordResetProperties(
        Duration tokenTtl
) {
    public PasswordResetProperties {
        if (tokenTtl == null || tokenTtl.isZero() || tokenTtl.isNegative()) {
            tokenTtl = Duration.ofHours(24);
        }
    }
}