package ru.just.securityservice.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import ru.just.dtolib.jwt.Tokens;
import ru.just.securityservice.security.userdetails.TokenUser;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthService {
    private final SecurityService securityService;
    private final RefreshTokenService refreshTokenService;

    public void logout(Authentication authentication) {
        final DecodedJWT token = getRefreshTokenFromAuth(authentication);

        refreshTokenService.deleteById(UUID.fromString(token.getId()));
    }

    private DecodedJWT getRefreshTokenFromAuth(Authentication authentication) {
        if (!isAuthenticationCorrect(authentication)) {
            throw new AccessDeniedException("Authentication incorrect. Use refresh JWT token");
        }

        return securityService.getVerifiedDecodedJwt(authentication.getCredentials().toString());
    }

    private static boolean isAuthenticationCorrect(Authentication authentication) {
        return authentication instanceof PreAuthenticatedAuthenticationToken
                && authentication.getPrincipal() instanceof TokenUser
                && authentication.getAuthorities().contains(new SimpleGrantedAuthority("JWT_REFRESH"));
    }

    public Tokens refresh(Authentication authentication) {
        DecodedJWT token = getRefreshTokenFromAuth(authentication);
        final UUID tokenId = UUID.fromString(token.getId());
        final UUID deviceId = UUID.fromString(token.getClaim(SecurityService.DEVICE_ID_CLAIM).asString());
        final long userId = Long.parseLong(token.getSubject());

        if (!refreshTokenService.isRefreshTokenValid(tokenId, deviceId, userId)) {
            refreshTokenService.deleteAllTokensByUserId(userId);
            throw new AccessDeniedException("Token invalid. Re-authenticate.");
        }

        String newRefreshToken = securityService.generateRefresh(authentication);
        final DecodedJWT decodedRefresh = JWT.decode(newRefreshToken);
        String accessToken = securityService.generateAccess(decodedRefresh);
        final DecodedJWT decodedAccess = JWT.decode(accessToken);

        refreshTokenService.deleteById(tokenId);
        refreshTokenService.saveIssuedRefreshToken(userId, token);
        return new Tokens(accessToken, decodedAccess.getExpiresAtAsInstant().toString(),
                newRefreshToken, decodedRefresh.getExpiresAtAsInstant().toString());
    }
}
