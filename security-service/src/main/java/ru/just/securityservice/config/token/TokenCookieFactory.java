package ru.just.securityservice.config.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import ru.just.securityservice.model.Token;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

@Component
public class TokenCookieFactory implements Function<Authentication, Token> {

    private final Duration tokenTtl;

    public TokenCookieFactory(@Value("${jwt.token-ttl-in-minutes}") Integer tokenTtlInMinutes) {
        this.tokenTtl = Duration.ofMinutes(tokenTtlInMinutes);
    }

    @Override
    public Token apply(Authentication authentication) {
        Instant now = Instant.now();
        return new Token(UUID.randomUUID(), authentication.getName(),
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList(),
                now, now.plus(tokenTtl));
    }
}
