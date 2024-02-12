package ru.just.securityservice.config.token;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import ru.just.securityservice.config.token.model.Token;
import ru.just.securityservice.repository.RefreshTokenRepository;
import ru.just.securityservice.repository.UserRepository;

import java.util.function.Function;

@Builder
public class TokenJwtAuthenticationConfigurer
        extends AbstractHttpConfigurer<TokenJwtAuthenticationConfigurer, HttpSecurity> {
    private Function<String, Token> accessDeserializer;
    private Function<String, Token> refreshDeserializer;
    private Function<Token, String> refreshTokenStringSerializer;
    private Function<Token, String> accessTokenStringSerializer;
    private AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService;
    private Function<Token, Token> accessTokenFactory;
    private Function<Authentication, Token> refreshTokenFactory;
    private RefreshTokenRepository refreshTokenRepository;
    private UserRepository userRepository;

    @Override
    public void init(HttpSecurity builder) {
        var csrfConfigurer = builder.getConfigurer(CsrfConfigurer.class);
        if (csrfConfigurer != null) {
            csrfConfigurer.ignoringRequestMatchers(new AntPathRequestMatcher("/jwt/tokens", "POST"));
        }
    }

    @Override
    public void configure(HttpSecurity builder) {
        var requestJwtTokensFilter = new RequestJwtTokensFilter(
                refreshTokenFactory, accessTokenFactory, refreshTokenRepository, userRepository);
        requestJwtTokensFilter.setAccessTokenStringSerializer(this.accessTokenStringSerializer);
        requestJwtTokensFilter.setRefreshTokenStringSerializer(this.refreshTokenStringSerializer);

        var jwtAuthenticationFilter = new AuthenticationFilter(builder.getSharedObject(AuthenticationManager.class),
                new TokenJwtAuthenticationConverter(accessDeserializer, refreshDeserializer));
        jwtAuthenticationFilter
                .setSuccessHandler((request, response, authentication) -> CsrfFilter.skipRequest(request));
        jwtAuthenticationFilter
                .setFailureHandler((request, response, exception) -> response.sendError(HttpServletResponse.SC_FORBIDDEN));

        var authenticationProvider = new PreAuthenticatedAuthenticationProvider();
        authenticationProvider.setPreAuthenticatedUserDetailsService(userDetailsService);

        var refreshTokenFilter = new RefreshTokenFilter(accessTokenFactory, refreshTokenFactory, refreshTokenRepository);
        refreshTokenFilter.setAccessTokenStringSerializer(this.accessTokenStringSerializer);
        refreshTokenFilter.setRefreshTokenStringSerializer(this.refreshTokenStringSerializer);

        var jwtLogoutFilter = new JwtLogoutFilter(refreshTokenRepository);

        builder.addFilterAfter(requestJwtTokensFilter, ExceptionTranslationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, CsrfFilter.class)
                .addFilterAfter(refreshTokenFilter, ExceptionTranslationFilter.class)
                .addFilterAfter(jwtLogoutFilter, ExceptionTranslationFilter.class)
                .authenticationProvider(authenticationProvider);
    }
}
