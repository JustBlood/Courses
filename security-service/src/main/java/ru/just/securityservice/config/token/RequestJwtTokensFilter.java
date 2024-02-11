package ru.just.securityservice.config.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.just.dtolib.jwt.Tokens;
import ru.just.securityservice.model.Token;

import java.io.IOException;
import java.util.function.Function;

@Setter
public class RequestJwtTokensFilter extends OncePerRequestFilter {
    private RequestMatcher requestMatcher = new AntPathRequestMatcher("/jwt/tokens", HttpMethod.POST.name());
    private SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();
    private final Function<Authentication, Token> refreshTokenFactory;
    private final Function<Token, Token> accessTokenFactory;
    private Function<Token, String> accessTokenStringSerializer = Object::toString;
    private Function<Token, String> refreshTokenStringSerializer = Object::toString;
    private ObjectMapper objectMapper = new ObjectMapper();

    public RequestJwtTokensFilter(Function<Authentication, Token> refreshTokenFactory, Function<Token, Token> accessTokenFactory) {
        this.refreshTokenFactory = refreshTokenFactory;
        this.accessTokenFactory = accessTokenFactory;
    }

    // todo: тут добавить репозиторий/сервис управления токенами в БД
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final SecurityContext securityContext = this.securityContextRepository.loadDeferredContext(request).get();
        if (isUserNotAuthenticated(request, securityContext)) {
            throw new AccessDeniedException("User must be authenticated");
        }
        // todo: сохранять токены в БД
        var refreshToken = this.refreshTokenFactory.apply(securityContext.getAuthentication());
        var accessToken = this.accessTokenFactory.apply(refreshToken);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        this.objectMapper.writeValue(response.getWriter(),
                new Tokens(this.accessTokenStringSerializer.apply(accessToken),
                        accessToken.expiresAt().toString(),
                        this.refreshTokenStringSerializer.apply(refreshToken),
                        refreshToken.expiresAt().toString()));
        return;
    }

    private boolean isUserNotAuthenticated(HttpServletRequest request, SecurityContext securityContext) {
        return !this.securityContextRepository.containsContext(request)
                || securityContext == null || securityContext.getAuthentication() instanceof PreAuthenticatedAuthenticationToken;
    }
}
