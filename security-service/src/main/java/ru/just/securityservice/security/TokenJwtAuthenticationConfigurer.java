package ru.just.securityservice.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import ru.just.securityservice.repository.UserRepository;
import ru.just.securityservice.service.RefreshTokenService;
import ru.just.securityservice.service.SecurityService;

@Builder
public class TokenJwtAuthenticationConfigurer
        extends AbstractHttpConfigurer<TokenJwtAuthenticationConfigurer, HttpSecurity> {
    private AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService;
    private UserRepository userRepository;
    private SecurityService securityService;
    private RefreshTokenService refreshTokenService;

    @Override
    public void init(HttpSecurity builder) {
        var csrfConfigurer = builder.getConfigurer(CsrfConfigurer.class);
        if (csrfConfigurer != null) {
            csrfConfigurer.ignoringRequestMatchers(new AntPathRequestMatcher("/jwt/tokens", "POST"));
        }
    }

    @Override
    public void configure(HttpSecurity builder) {
        // todo: можно перенести в секьюрити-конфиг
        var requestJwtTokensFilter = new RequestJwtTokensFilter(securityService, refreshTokenService);

        var jwtAuthenticationFilter = new AuthenticationFilter(builder.getSharedObject(AuthenticationManager.class),
                new TokenJwtAuthenticationConverter(securityService));
        jwtAuthenticationFilter // todo: csrf politics
                .setSuccessHandler((request, response, authentication) -> CsrfFilter.skipRequest(request));
        jwtAuthenticationFilter
                .setFailureHandler((request, response, exception) -> response.sendError(HttpServletResponse.SC_FORBIDDEN));

        var authenticationProvider = new PreAuthenticatedAuthenticationProvider();
        authenticationProvider.setPreAuthenticatedUserDetailsService(userDetailsService);

        builder.addFilterAfter(requestJwtTokensFilter, ExceptionTranslationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, CsrfFilter.class)
                .authenticationProvider(authenticationProvider);
    }
}
