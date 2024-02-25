package ru.just.securityservice.config;

import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import ru.just.securityservice.repository.UserRepository;
import ru.just.securityservice.security.TokenJwtAuthenticationConfigurer;
import ru.just.securityservice.service.RefreshTokenService;
import ru.just.securityservice.service.SecurityService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {
    @Bean
    public Algorithm algorithm(@Value("${jwt.secret}") String secret) {
        return Algorithm.HMAC256(secret);
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public TokenJwtAuthenticationConfigurer jwtAuthenticationConfigurer(
            AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService,
            RefreshTokenService refreshTokenService,
            UserRepository userRepository,
            SecurityService securityService
    ) {
        return TokenJwtAuthenticationConfigurer.builder()
                .userDetailsService(userDetailsService)
                .refreshTokenService(refreshTokenService)
                .userRepository(userRepository)
                .securityService(securityService)
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           TokenJwtAuthenticationConfigurer tokenJwtAuthenticationConfigurer) throws Exception {
        return http
                .authorizeHttpRequests(authorizeHttpRequests ->
                        authorizeHttpRequests
                                .requestMatchers("/api/v1/auth/register",
                                        "/api/v1/auth/login",
                                        "api/v1/auth/token/validate").permitAll()
                                .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .with(tokenJwtAuthenticationConfigurer, Customizer.withDefaults())
                .build();
    }
}
