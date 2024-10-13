package ru.just.securitylib.service;

import com.auth0.jwt.interfaces.DecodedJWT;

public class ThreadLocalTokenService {
    ThreadLocal<DecodedJWT> jwtToken = new ThreadLocal<>();

    public DecodedJWT getDecodedToken() {
        return jwtToken.get();
    }

    public Long getUserId() {
        if (jwtToken.get() != null) {
            return Long.parseLong(jwtToken.get().getSubject());
        }
        return null;
    }

    public void setDecodedToken(DecodedJWT jwtToken) {
        this.jwtToken.set(jwtToken);
    }
}
