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
import org.springframework.http.HttpStatus;
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
import ru.just.securityservice.dto.DeviceIdPayload;
import ru.just.securityservice.model.RefreshToken;
import ru.just.securityservice.service.RefreshTokenService;
import ru.just.securityservice.service.SecurityService;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Setter
public class RequestJwtTokensFilter extends OncePerRequestFilter {
    private RequestMatcher requestMatcher = new AntPathRequestMatcher("/api/v1/auth/login", HttpMethod.POST.name());
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

        final Optional<UUID> deviceId = getDeviceId(request, securityContext.getAuthentication());
        if (deviceId.isEmpty()) {
            response.sendError(HttpStatus.BAD_REQUEST.value());
            return;
        }

        var refreshToken = securityService.generateRefresh(securityContext.getAuthentication());
        final DecodedJWT decodedRefresh = JWT.decode(refreshToken);
        var accessToken = securityService.generateAccess(decodedRefresh);
        final DecodedJWT decodedAccess = JWT.decode(accessToken);

        refreshTokenService.deleteTokenByUserIdAndDeviceId(Long.parseLong(decodedRefresh.getSubject()), deviceId.get());
        final long userId = Long.parseLong(decodedAccess.getSubject());
        refreshTokenService.saveIssuedRefreshToken(userId, decodedRefresh, deviceId.get());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        this.objectMapper.writeValue(response.getWriter(),
                new Tokens(accessToken,
                        decodedAccess.getExpiresAtAsInstant().toString(),
                        refreshToken,
                        decodedRefresh.getExpiresAtAsInstant().toString()));
    }

    private Optional<UUID> getDeviceId(HttpServletRequest request, Authentication authentication) {
        if (authentication instanceof PreAuthenticatedAuthenticationToken) {
            final DecodedJWT jwt = securityService.getVerifiedDecodedJwt(authentication.getCredentials().toString());
            return refreshTokenService.findById(UUID.fromString(jwt.getId()))
                    .map(RefreshToken::getDeviceId);
        } else {
            try {
                return Optional.of(objectMapper.readValue(request.getInputStream(), DeviceIdPayload.class).getDeviceId());
            } catch (IOException e) {
                return Optional.empty();
            }
        }
    }

    private boolean isUserNotAuthenticated(HttpServletRequest request, SecurityContext securityContext) {
        return !this.securityContextRepository.containsContext(request)
                || securityContext == null;
    }
}
