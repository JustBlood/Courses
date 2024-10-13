package ru.just.securitylib.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.just.securitylib.filter.TokenServiceExchangeFilter;
import ru.just.securitylib.service.ThreadLocalTokenService;

@Configuration
@ConditionalOnClass(ThreadLocalTokenService.class)
public class PreAuthenticatedTokenAutoConfiguration {

    @Bean
    public TokenServiceExchangeFilter tokenServiceExchangeFilter(ThreadLocalTokenService tokenService) {
        return new TokenServiceExchangeFilter(tokenService);
    }

    @Bean
    public ThreadLocalTokenService tokenService() {
        return new ThreadLocalTokenService();
    }

}
