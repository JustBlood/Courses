package ru.just.userservice.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Service;

@Service
public class ThreadLocalTokenService {
    ThreadLocal<DecodedJWT> jwtToken = new ThreadLocal<>();

    public DecodedJWT getToken() {
        return jwtToken.get();
    }

    public Long getUserId() {
        if (jwtToken.get() != null) {
            return Long.parseLong(jwtToken.get().getSubject());
        }
        return null;
    }

    public void setToken(DecodedJWT jwtTokem) {
        jwtToken.set(jwtTokem);
    }
}
