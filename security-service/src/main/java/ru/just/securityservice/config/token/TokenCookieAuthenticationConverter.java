package ru.just.securityservice.config.token;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import ru.just.securityservice.model.Token;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class TokenCookieAuthenticationConverter implements AuthenticationConverter {

    private final Function<String, Token> tokenCookieStringDeserializer;
    private final String COOKIE_NAME;

    @Override
    public Authentication convert(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        return Stream.of(request.getCookies())
                .filter(cookie -> cookie.getName().equals(COOKIE_NAME))
                .findFirst()
                .map(cookie -> {
                    Token token = tokenCookieStringDeserializer.apply(cookie.getValue());
                    return new PreAuthenticatedAuthenticationToken(token, cookie.getValue());
                })
                .orElse(null);
    }
}
