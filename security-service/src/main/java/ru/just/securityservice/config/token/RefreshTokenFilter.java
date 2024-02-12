package ru.just.securityservice.config.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.just.dtolib.jwt.Tokens;
import ru.just.securityservice.config.token.model.Token;
import ru.just.securityservice.config.token.model.TokenUser;
import ru.just.securityservice.model.RefreshToken;
import ru.just.securityservice.repository.RefreshTokenRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.function.Function;

@RequiredArgsConstructor
@Setter
public class RefreshTokenFilter extends OncePerRequestFilter {
    private RequestMatcher requestMatcher = new AntPathRequestMatcher("/jwt/refresh", HttpMethod.POST.name());
    private SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();
    private final Function<Token, Token> accessTokenFactory;
    private final Function<Authentication, Token> refreshTokenFactory;
    private Function<Token, String> accessTokenStringSerializer = Object::toString;
    private Function<Token, String> refreshTokenStringSerializer = Object::toString;
    private ObjectMapper objectMapper = new ObjectMapper();
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        var context = this.securityContextRepository.loadDeferredContext(request).get();
        if (isUserNotValidJwtAuth(request, context))
            throw new AccessDeniedException("User must be authenticated with JWT");

        TokenUser tokenUser = (TokenUser) context.getAuthentication().getPrincipal();
        final Token token = tokenUser.getToken();
        if (!refreshTokenRepository.existsByIdAndExpiresAtAfterAndDeviceIdAndUser_UserId(
                token.id(), Instant.now(), token.deviceId(), tokenUser.getUser().getUserId())) {
            refreshTokenRepository.deleteByUserUserId(tokenUser.getUser().getUserId());
            throw new AccessDeniedException("Token invalid. Re-authenticate.");
        }

        var refreshToken = this.refreshTokenFactory.apply(context.getAuthentication());
        var accessToken = this.accessTokenFactory.apply(refreshToken);

        refreshTokenRepository.deleteById(token.id());
        saveIssuedRefreshToken(context, refreshToken);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        this.objectMapper.writeValue(response.getWriter(),
                new Tokens(this.accessTokenStringSerializer.apply(accessToken), accessToken.expiresAt().toString(),
                        this.refreshTokenStringSerializer.apply(refreshToken), refreshToken.expiresAt().toString()));
        return;
    }

    private void saveIssuedRefreshToken(SecurityContext securityContext, Token refreshToken) {
        TokenUser user = (TokenUser) securityContext.getAuthentication().getPrincipal();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .id(refreshToken.id())
                .user(user.getUser())
                .createdAt(refreshToken.createdAt())
                .expiresAt(refreshToken.expiresAt())
                .deviceId(refreshToken.deviceId())
                .build();

        refreshTokenRepository.save(refreshTokenEntity);
    }

    private boolean isUserNotValidJwtAuth(HttpServletRequest request, SecurityContext context) {
        return !securityContextRepository.containsContext(request)
                || context == null || !(context.getAuthentication() instanceof PreAuthenticatedAuthenticationToken)
                || !(context.getAuthentication().getPrincipal() instanceof TokenUser)
                || !(context.getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("JWT_REFRESH")));
    }

}
