package ru.just.securityservice.config.token;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import ru.just.securityservice.config.token.model.Token;
import ru.just.securityservice.config.token.model.TokenUser;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.UUID;
import java.util.function.Function;

@Component("refresh")
@Setter
public class RefreshTokenFactory implements Function<Authentication, Token> {
    @Value("#{T(java.time.Duration).ofDays(${jwt.refresh-ttl-in-days})}")
    private Duration tokenTtlInDays;

    @Override
    public Token apply(Authentication authentication) {
        final Instant createdAt = Instant.now();
        var authorities = new LinkedList<String>();
        authorities.add("JWT_REFRESH");
        authorities.add("JWT_LOGOUT");
        authentication.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority)
                .map(authority -> "GRANT_" + authority)
                .forEach(authorities::add);
        UUID deviceId = UUID.randomUUID();
        if (authentication.getPrincipal() instanceof TokenUser tokenUser) {
            deviceId = tokenUser.getToken().deviceId();
        }
        return new Token(UUID.randomUUID(), authentication.getName(), deviceId, authorities, createdAt,
                createdAt.plus(tokenTtlInDays));
    }
}
