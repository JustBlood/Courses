package ru.just.monolithmvp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {
    public AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("Unauthenticated request");
        }
        return user;
    }

    public Long currentUserId() {
        return currentUser().getId();
    }

    public String resolveCurrentActor() {
        try {
            return currentUser().getUsername();
        } catch (Exception ex) {
            return "system";
        }
    }
}
