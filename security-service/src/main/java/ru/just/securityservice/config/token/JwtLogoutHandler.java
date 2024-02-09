package ru.just.securityservice.config.token;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import ru.just.securityservice.model.TokenUser;

public class JwtLogoutHandler implements LogoutHandler {

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null &&
                authentication.getPrincipal() instanceof TokenUser user) {
            // TODO:надо хранить токены и тут их сбрасывать, деактивировать

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }
}
