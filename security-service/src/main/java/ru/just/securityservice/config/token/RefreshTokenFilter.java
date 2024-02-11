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
import ru.just.securityservice.model.Token;
import ru.just.securityservice.model.TokenUser;

import java.io.IOException;
import java.util.function.Function;

@RequiredArgsConstructor
@Setter
public class RefreshTokenFilter extends OncePerRequestFilter {
    private RequestMatcher requestMatcher = new AntPathRequestMatcher("/jwt/refresh", HttpMethod.POST.name());
    private SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();
    private final Function<Token, Token> accessTokenFactory;
    private final Function<Authentication, Token> refreshTokenFactory;
    private Function<Token, String> accessTokenStringSerializer = Object::toString;
    private ObjectMapper objectMapper = new ObjectMapper();
    // todo: тут добавить репозиторий/сервис управления токенами в БД
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
        //todo: надо обновлять и рефреш-токен + работа с БД
        TokenUser user = (TokenUser) context.getAuthentication().getPrincipal();
        var accessToken = this.accessTokenFactory.apply(user.getToken());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        this.objectMapper.writeValue(response.getWriter(),
                new Tokens(this.accessTokenStringSerializer.apply(accessToken),
                        accessToken.expiresAt().toString(), null, null));
        return;
    }

    private boolean isUserNotValidJwtAuth(HttpServletRequest request, SecurityContext context) {
        return !securityContextRepository.containsContext(request)
                || context == null || !(context.getAuthentication() instanceof PreAuthenticatedAuthenticationToken)
                || !(context.getAuthentication().getPrincipal() instanceof TokenUser)
                || !(context.getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("JWT_REFRESH")));
    }

}
