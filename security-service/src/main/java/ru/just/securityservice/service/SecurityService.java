package ru.just.securityservice.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.just.dtolib.kafka.users.UserDeliverStatus;
import ru.just.securityservice.repository.UserRepository;
import ru.just.securityservice.security.userdetails.TokenUser;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class SecurityService {
    public static final String AUTHORITIES_CLAIM = "authorities";
    public static final String ROLE_PREFIX = "GRANT_";
    public static final String ISSUER = "just-company";
    public static final String JWT_REFRESH_CLAIM = "JWT_REFRESH";
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final Algorithm algorithm;
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.refresh-ttl-in-days}")
    private Integer refreshTtlInDays;
    @Value("${jwt.access-ttl-in-seconds}")
    private Integer accessTtlInSeconds;

    public String generateRefresh(Authentication authentication) {
        final Instant now = Instant.now();

        var authorities = new LinkedList<String>();
        authorities.add(JWT_REFRESH_CLAIM);
        authorities.add("JWT_LOGOUT");
        authentication.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority)
                .filter(authority -> !authorities.contains(authority))
                .map(authority -> authority.startsWith(ROLE_PREFIX) ? authority : ROLE_PREFIX + authority)
                .forEach(authorities::add);

        String userId = getUserIdFromAuthentication(authentication);

        return JWT.create()
                .withIssuer(ISSUER)
                .withJWTId(UUID.randomUUID().toString())
                .withSubject(userId)
                .withExpiresAt(now.plus(Duration.ofDays(refreshTtlInDays)))
                .withIssuedAt(now)
                .withClaim(AUTHORITIES_CLAIM, authorities)
                .sign(algorithm);
    }

    private String getUserIdFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof TokenUser tokenUser) {
            return tokenUser.getUser().getUserId().toString();
        }
        final User user = (User) authentication.getPrincipal();
        return userRepository.findByLoginAndDeliverStatus(user.getUsername(), UserDeliverStatus.DELIVERED)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                .getUserId().toString();
    }

    public String generateAccess(DecodedJWT refreshToken) {
        final Instant now = Instant.now();

        List<String> authorities = refreshToken.getClaim(AUTHORITIES_CLAIM).asList(String.class).stream()
                .filter(c -> c.startsWith(ROLE_PREFIX))
                .map(c -> c.replace(ROLE_PREFIX, ""))
                .toList();

        return JWT.create()
                .withIssuer(ISSUER)
                .withJWTId(UUID.randomUUID().toString())
                .withIssuer(refreshToken.getIssuer())
                .withSubject(refreshToken.getSubject())
                .withExpiresAt(now.plus(Duration.ofSeconds(accessTtlInSeconds)))
                .withIssuedAt(now)
                .withClaim(AUTHORITIES_CLAIM, authorities)
                .sign(algorithm);
    }

    public DecodedJWT getVerifiedDecodedJwt(String rawToken) {
        final DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(rawToken);
        if (jwt.getClaim(AUTHORITIES_CLAIM).asList(String.class).contains(JWT_REFRESH_CLAIM)) {
            refreshTokenService.findById(UUID.fromString(jwt.getId()))
                    .orElseThrow(() -> new AccessDeniedException("Refresh token invalid"));
        }
        return jwt;
    }

    public void validateAccessToken(String token) {
        try {
            DecodedJWT jwt = getVerifiedDecodedJwt(token);
            if (jwt.getExpiresAtAsInstant().isBefore(Instant.now())) {
                throw new JWTVerificationException("Jwt token expired");
            }
            if (jwt.getClaim(AUTHORITIES_CLAIM).asList(String.class).contains(JWT_REFRESH_CLAIM)) {
                throw new JWTVerificationException("Refresh token can't be validated");
            }
        } catch (JWTVerificationException verificationException) {
            throw new AccessDeniedException(verificationException.getMessage());
        }
    }
}
