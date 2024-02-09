package ru.just.securityservice.config.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.stereotype.Component;
import ru.just.securityservice.model.Token;

import java.util.function.Function;

@Component
public class TokenCookieAuthenticationConfigurer
        extends AbstractHttpConfigurer<TokenCookieAuthenticationConfigurer, HttpSecurity> {

    private final Function<String, Token> tokenCookieStringDeserializer;
    private final AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService;
    private final String COOKIE_NAME;

    public TokenCookieAuthenticationConfigurer(Function<String, Token> tokenCookieStringDeserializer,
                                               AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService,
                                               @Value("${jwt.cookie-name}") String cookieName) {
        this.tokenCookieStringDeserializer = tokenCookieStringDeserializer;
        this.userDetailsService = userDetailsService;
        COOKIE_NAME = cookieName;
    }

    @Override
    public void init(HttpSecurity builder) throws Exception {
        builder.logout(logout -> logout.addLogoutHandler(new CookieClearingLogoutHandler(COOKIE_NAME))
                .addLogoutHandler(new JwtLogoutHandler()));
    }

    @Override
    public void configure(HttpSecurity builder) {
        var cookieAuthenticationFilter = new AuthenticationFilter(
                builder.getSharedObject(AuthenticationManager.class),
                new TokenCookieAuthenticationConverter(tokenCookieStringDeserializer, COOKIE_NAME));
        cookieAuthenticationFilter.setSuccessHandler((request, response, authentication) -> {});
        cookieAuthenticationFilter.setFailureHandler(new AuthenticationEntryPointFailureHandler(
                new Http403ForbiddenEntryPoint()));

        var authenticationProvider = new PreAuthenticatedAuthenticationProvider();
        authenticationProvider.setPreAuthenticatedUserDetailsService(userDetailsService);

        builder.addFilterAfter(cookieAuthenticationFilter, CsrfFilter.class)
                .authenticationProvider(authenticationProvider);
    }
}
