package ru.just.securityservice.config.token;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import ru.just.securityservice.config.token.model.Token;

import java.util.function.Function;

@RequiredArgsConstructor
public class TokenJwtAuthenticationConverter implements AuthenticationConverter {

    private final Function<String, Token> accessTokenDeserializer;
    private final Function<String, Token> refreshTokenDeserializer;

    @Override
    public Authentication convert(HttpServletRequest request) {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        return getTokenIfAuthorizationCorrect(authorization);
    }

    private PreAuthenticatedAuthenticationToken getTokenIfAuthorizationCorrect(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.replace("Bearer ", "");
            Token accessToken = accessTokenDeserializer.apply(token);
            if (accessToken != null) {
                return new PreAuthenticatedAuthenticationToken(accessToken, token);
            }

            Token refreshToken = refreshTokenDeserializer.apply(token);
            if (refreshToken != null) {
                return new PreAuthenticatedAuthenticationToken(refreshToken, token);
            }
        }
        return null;
    }
}
