package ru.just.securityservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.just.dtolib.jwt.Tokens;
import ru.just.securityservice.service.RefreshTokenService;
import ru.just.securityservice.service.SecurityService;

import java.io.IOException;

@Setter
public class RequestJwtTokensFilter extends OncePerRequestFilter {
    private RequestMatcher requestMatcher = new AntPathRequestMatcher("/jwt/tokens", HttpMethod.POST.name());
    private SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();
    private ObjectMapper objectMapper = new ObjectMapper();
    private final SecurityService securityService;
    private final RefreshTokenService refreshTokenService;

    public RequestJwtTokensFilter(SecurityService securityService, RefreshTokenService refreshTokenService) {
        this.securityService = securityService;
        this.refreshTokenService = refreshTokenService;
    }

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

        var refreshToken = securityService.generateRefresh(securityContext.getAuthentication());
        final DecodedJWT decodedRefresh = JWT.decode(refreshToken);
        var accessToken = securityService.generateAccess(decodedRefresh);
        final DecodedJWT decodedAccess = JWT.decode(accessToken);

        User user = (User) securityContext.getAuthentication().getPrincipal();
        refreshTokenService.saveIssuedRefreshToken(user, decodedRefresh);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        this.objectMapper.writeValue(response.getWriter(),
                new Tokens(accessToken,
                        decodedAccess.getExpiresAtAsInstant().toString(),
                        refreshToken,
                        decodedRefresh.getExpiresAtAsInstant().toString()));
    }

    private boolean isUserNotAuthenticated(HttpServletRequest request, SecurityContext securityContext) {
        return !this.securityContextRepository.containsContext(request)
                || securityContext == null || securityContext.getAuthentication() instanceof PreAuthenticatedAuthenticationToken;
    }
}
