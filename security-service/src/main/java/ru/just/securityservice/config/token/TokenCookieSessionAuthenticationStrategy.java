package ru.just.securityservice.config.token;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Component;
import ru.just.securityservice.model.Token;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class TokenCookieSessionAuthenticationStrategy implements SessionAuthenticationStrategy {

    private final Function<Authentication, Token> tokenCookieFactory;
    private final Function<Token, String> tokenStringSerializer;

    @Override
    public void onAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) throws SessionAuthenticationException {
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            Token token = tokenCookieFactory.apply(authentication);
            String tokenString = tokenStringSerializer.apply(token);

            Cookie cookie = new Cookie("__Host_auth-token", tokenString);
            cookie.setPath("/");
            cookie.setDomain(null);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);

            cookie.setMaxAge((int) ChronoUnit.SECONDS.between(Instant.now(), token.expiredAt()));

            response.addCookie(cookie);
        }
    }
}
