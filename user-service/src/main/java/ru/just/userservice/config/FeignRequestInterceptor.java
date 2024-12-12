package ru.just.userservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import ru.just.securitylib.service.ThreadLocalTokenService;

@Component
@RequiredArgsConstructor
public class FeignRequestInterceptor implements RequestInterceptor {
    private final ThreadLocalTokenService tokenService;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        if (tokenService.getDecodedToken() != null) {
            requestTemplate.header(HttpHeaders.AUTHORIZATION, tokenService.getDecodedToken().getToken());
        }
    }
}

