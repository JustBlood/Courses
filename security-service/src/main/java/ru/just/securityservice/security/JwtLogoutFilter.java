package ru.just.securityservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import ru.just.securityservice.service.RefreshTokenService;
import ru.just.securityservice.service.SecurityService;

import java.util.UUID;

@Setter
public class JwtLogoutFilter extends CheckRefreshTokenFilter {
    public JwtLogoutFilter(SecurityService securityService, RefreshTokenService refreshTokenService) {
        super(new AntPathRequestMatcher("/jwt/logout", HttpMethod.POST.name()), securityService, refreshTokenService);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) {
        UUID refreshTokenId = UUID.fromString(refreshToken.getId());
        refreshTokenService.deleteById(refreshTokenId);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
