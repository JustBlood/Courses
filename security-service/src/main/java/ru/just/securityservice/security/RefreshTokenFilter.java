package ru.just.securityservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import java.util.UUID;

@RequiredArgsConstructor
@Setter
public class RefreshTokenFilter extends OncePerRequestFilter {
    private RequestMatcher requestMatcher = new AntPathRequestMatcher("/jwt/refresh", HttpMethod.POST.name());
    private SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();
    private ObjectMapper objectMapper = new ObjectMapper();
    private final SecurityService securityService;
    private final RefreshTokenService refreshTokenService;

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
        // todo: переделать UserDetails и поместить внутрь токен
        DecodedJWT refreshToken = securityService.getVerifiedDecodedJwt(context.getAuthentication().getCredentials().toString());
        var userId = Long.parseLong(refreshToken.getSubject());
        if (!refreshTokenService.isRefreshTokenValid(
                UUID.fromString(refreshToken.getId()),
                UUID.fromString(refreshToken.getClaim(SecurityService.DEVICE_ID_CLAIM).asString()),
                userId)) {
            refreshTokenService.deleteByUserId(userId); //todo: исправить, почему-то сейчас метод не работает
            throw new AccessDeniedException("Token invalid. Re-authenticate.");
        }

        String newRefreshToken = securityService.generateRefresh(context.getAuthentication());
        final DecodedJWT decodedRefresh = JWT.decode(newRefreshToken);
        String accessToken = securityService.generateAccess(decodedRefresh);
        final DecodedJWT decodedAccess = JWT.decode(accessToken);

        refreshTokenService.deleteById(UUID.fromString(refreshToken.getId()));
        refreshTokenService.saveIssuedRefreshToken(context, refreshToken);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        this.objectMapper.writeValue(response.getWriter(),
                new Tokens(accessToken, decodedAccess.getExpiresAtAsInstant().toString(),
                        newRefreshToken, decodedRefresh.getExpiresAtAsInstant().toString()));
        return;
    }

    private boolean isUserNotValidJwtAuth(HttpServletRequest request, SecurityContext context) {
        return !securityContextRepository.containsContext(request)
                || context == null || !(context.getAuthentication() instanceof PreAuthenticatedAuthenticationToken)
                || !(context.getAuthentication().getPrincipal() instanceof User)
                || !(context.getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("JWT_REFRESH")));
    }

}
