package ru.just.securitylib.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.io.IOException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TokenServiceExchangeFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private final ThreadLocalTokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null) {
            bearerToken = bearerToken.replace(BEARER_PREFIX, "");
            final DecodedJWT decodedJWT = JWT.decode(bearerToken);
            tokenService.setDecodedToken(decodedJWT);
        }
        filterChain.doFilter(request, response);
    }
}
