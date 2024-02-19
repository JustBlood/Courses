package ru.just.securityservice.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.just.securityservice.service.RefreshTokenService;
import ru.just.securityservice.service.SecurityService;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
public class CheckRefreshTokenFilter extends OncePerRequestFilter {
    protected final RequestMatcher requestMatcher;
    protected final SecurityService securityService;
    protected final RefreshTokenService refreshTokenService;
    private final SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();
    protected Authentication authentication;
    protected DecodedJWT refreshToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        var authentication = getAuthenticationFromSecurityContext(request);
        if (!(authentication instanceof PreAuthenticatedAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }
        final DecodedJWT refreshToken = securityService.getVerifiedDecodedJwt(authentication.getCredentials().toString());
        final UUID tokenId = UUID.fromString(refreshToken.getId());
        final UUID deviceId = UUID.fromString(refreshToken.getClaim(SecurityService.DEVICE_ID_CLAIM).asString());
        final long userId = Long.parseLong(refreshToken.getSubject());

        if (!refreshTokenService.isRefreshTokenValid(tokenId, deviceId, userId)) {
            refreshTokenService.deleteByUserId(userId);
            throw new AccessDeniedException("Token invalid. Re-authenticate.");
        }
    }

    private Authentication getAuthenticationFromSecurityContext(HttpServletRequest request) {
        if (!this.securityContextRepository.containsContext(request)) {
            throw new AccessDeniedException("User must be authenticated with JWT");
        }
        var context = this.securityContextRepository.loadDeferredContext(request).get();
        if (context == null || !isAuthenticationCorrect(context.getAuthentication())) {
            throw new AccessDeniedException("Authentication incorrect. Use refresh token");
        }
        return context.getAuthentication();
    }

    private static boolean isAuthenticationCorrect(Authentication authentication) {
        return authentication.getPrincipal() instanceof User &&
                authentication.getAuthorities()
                        .contains(new SimpleGrantedAuthority("JWT_REFRESH"));
    }
}
