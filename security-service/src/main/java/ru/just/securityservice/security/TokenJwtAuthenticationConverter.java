package ru.just.securityservice.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import ru.just.securityservice.service.SecurityService;

@RequiredArgsConstructor
@Getter
@Setter
public class TokenJwtAuthenticationConverter implements AuthenticationConverter {

    public static final String BEARER_PREFIX = "Bearer ";
    private final SecurityService securityService;

    @Override
    public Authentication convert(HttpServletRequest request) {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        return getTokenIfAuthorizationCorrect(authorization);
    }

    private PreAuthenticatedAuthenticationToken getTokenIfAuthorizationCorrect(String authorization) {
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            String rawToken = authorization.replace(BEARER_PREFIX, "");
            DecodedJWT decodedJwtToken = securityService.getVerifiedDecodedJwt(rawToken);
            return new PreAuthenticatedAuthenticationToken(decodedJwtToken, rawToken);
        }
        return null;
    }
}
