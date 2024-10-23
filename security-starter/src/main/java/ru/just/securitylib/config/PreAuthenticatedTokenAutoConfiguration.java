package ru.just.securitylib.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import ru.just.securitylib.filter.TokenServiceExchangeFilter;
import ru.just.securitylib.service.ThreadLocalTokenService;

@Configuration
@ConditionalOnClass(ThreadLocalTokenService.class)
public class PreAuthenticatedTokenAutoConfiguration {

    @Bean
    @Order(1000)
    public TokenServiceExchangeFilter tokenServiceExchangeFilter(ThreadLocalTokenService tokenService) {
        return new TokenServiceExchangeFilter(tokenService);
    }

    @ConditionalOnMissingBean
    @Bean
    public ThreadLocalTokenService tokenService() {
        return new ThreadLocalTokenService();
    }

}
