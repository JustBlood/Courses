package ru.just.communicationservice.controller.interceptor;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import ru.just.communicationservice.security.SecurityService;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    private final SecurityService securityService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        String token = request.getURI().getQuery().split("=")[1];
        log.debug("request headers: {}", request.getHeaders());
        // Проверяем наличие токена
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("token is null or not Bearer");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // Валидация токена через security-service
        try {
            log.debug("received Bearer token in Authorization Header");
            if (!securityService.isValidToken(token.substring(7))) {
                log.warn("Bearer token is not valid");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
        } catch (Exception e) {
            log.error("Error while token validation", e);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        DecodedJWT jwt = JWT.decode(token.substring(7));
        log.debug("setting session attributes: userId: {}, jwt: {}", jwt.getSubject(), jwt.getToken());
        attributes.put("userId", jwt.getSubject());
        attributes.put("jwt", jwt.getToken());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Действия после рукопожатия (если необходимо)
    }
}
