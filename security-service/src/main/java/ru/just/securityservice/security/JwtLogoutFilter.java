package ru.just.securityservice.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.just.securityservice.service.RefreshTokenService;
import ru.just.securityservice.service.SecurityService;

import java.io.IOException;
import java.util.UUID;

@Setter
@RequiredArgsConstructor
public class JwtLogoutFilter extends OncePerRequestFilter {

    private RequestMatcher requestMatcher = new AntPathRequestMatcher("/jwt/logout", HttpMethod.POST.name());
    private SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();
    private final RefreshTokenService refreshTokenService;
    private final SecurityService securityService;
    // todo: тут добавить репозиторий/сервис управления токенами в БД
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // todo: рефакторинг
        if (this.requestMatcher.matches(request)) {
            if (this.securityContextRepository.containsContext(request)) {
                var context = this.securityContextRepository.loadDeferredContext(request).get();
                if (context != null && context.getAuthentication() instanceof PreAuthenticatedAuthenticationToken token &&
                        context.getAuthentication().getPrincipal() instanceof User &&
                        context.getAuthentication().getAuthorities()
                                .contains(new SimpleGrantedAuthority("JWT_LOGOUT"))) {
                    // todo: тут сделать удаление токена из активных в БД
                    final DecodedJWT verifiedDecodedJwt = securityService.getVerifiedDecodedJwt(token.getCredentials().toString());
                    if (!refreshTokenService.isRefreshTokenValid(UUID.fromString(verifiedDecodedJwt.getId()),
                            UUID.fromString(verifiedDecodedJwt.getClaim(SecurityService.DEVICE_ID_CLAIM).asString()),
                            Long.parseLong(verifiedDecodedJwt.getSubject()))) {
                        throw new AccessDeniedException("Token is invalid");
                    }
                    final String refreshTokenId = verifiedDecodedJwt.getId();
                    refreshTokenService.deleteById(UUID.fromString(refreshTokenId));
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    return;
                }
            }

            throw new AccessDeniedException("User must be authenticated with JWT");
        }

        filterChain.doFilter(request, response);
    }
}
