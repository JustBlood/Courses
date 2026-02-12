package ru.just.monolithmvp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import ru.just.monolithmvp.config.properties.JwtProperties;
import ru.just.monolithmvp.model.Role;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, Role role) {
        final Date now = new Date();
        final Date expiration = new Date(now.getTime() + jwtProperties.expirationMs());

        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String username) {
        final Claims claims = extractAllClaims(token);
        return claims.getSubject().equals(username) && claims.getExpiration().after(new Date());
    }
}
