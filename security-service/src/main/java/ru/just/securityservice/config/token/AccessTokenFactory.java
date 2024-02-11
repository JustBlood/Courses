package ru.just.securityservice.config.token;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.just.securityservice.model.Token;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

@Component("access")
@Setter
public class AccessTokenFactory implements Function<Token, Token> {
    @Value("#{T(java.time.Duration).ofSeconds(${jwt.access-ttl-in-seconds})}")
    private Duration tokenTtlInSeconds;

    @Override
    public Token apply(Token refreshToken) {
        var createdAt = Instant.now();
        return new Token(refreshToken.id(), refreshToken.subject(),
                refreshToken.authorities().stream()
                        .filter(authority -> authority.startsWith("GRANT_"))
                        .map(authority -> authority.replace("GRANT_", ""))
                        .toList(),
                createdAt,
                createdAt.plus(tokenTtlInSeconds));
    }
}
