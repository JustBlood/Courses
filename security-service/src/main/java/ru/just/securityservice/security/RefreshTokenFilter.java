package ru.just.securityservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import ru.just.dtolib.jwt.Tokens;
import ru.just.securityservice.service.RefreshTokenService;
import ru.just.securityservice.service.SecurityService;

import java.io.IOException;
import java.util.UUID;

@Setter
public class RefreshTokenFilter extends CheckRefreshTokenFilter {
    private ObjectMapper objectMapper = new ObjectMapper();

    public RefreshTokenFilter(SecurityService securityService, RefreshTokenService refreshTokenService) {
        super(new AntPathRequestMatcher("/jwt/refresh", HttpMethod.POST.name()), securityService, refreshTokenService);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws IOException {
        String newRefreshToken = securityService.generateRefresh(authentication);
        final DecodedJWT decodedRefresh = JWT.decode(newRefreshToken);
        String accessToken = securityService.generateAccess(decodedRefresh);
        final DecodedJWT decodedAccess = JWT.decode(accessToken);

        UUID tokenId = UUID.fromString(refreshToken.getId());
        refreshTokenService.deleteById(tokenId);
        refreshTokenService.saveIssuedRefreshToken((User) authentication.getPrincipal(), refreshToken);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        this.objectMapper.writeValue(response.getWriter(),
                new Tokens(accessToken, decodedAccess.getExpiresAtAsInstant().toString(),
                        newRefreshToken, decodedRefresh.getExpiresAtAsInstant().toString()));
    }
}
