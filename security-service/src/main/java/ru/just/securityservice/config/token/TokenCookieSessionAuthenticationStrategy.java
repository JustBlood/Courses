package ru.just.securityservice.config.token;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
public class TokenCookieSessionAuthenticationStrategy implements SessionAuthenticationStrategy {

    private final Function<Authentication, Token> tokenCookieFactory;
    private final Function<Token, String> tokenStringSerializer;
    private final String COOKIE_NAME;

    public TokenCookieSessionAuthenticationStrategy(Function<Authentication, Token> tokenCookieFactory,
                                                    Function<Token, String> tokenStringSerializer,
                                                    @Value("${jwt.cookie-name}") String cookieName) {
        this.tokenCookieFactory = tokenCookieFactory;
        this.tokenStringSerializer = tokenStringSerializer;
        COOKIE_NAME = cookieName;
    }

    @Override
    public void onAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) throws SessionAuthenticationException {
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            Token token = tokenCookieFactory.apply(authentication);
            String tokenString = tokenStringSerializer.apply(token);

            Cookie cookie = new Cookie(COOKIE_NAME, tokenString);
            cookie.setPath("/");
            cookie.setDomain(null);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);

            cookie.setMaxAge((int) ChronoUnit.SECONDS.between(Instant.now(), token.expiresAt()));

            response.addCookie(cookie);
        }
    }
}
