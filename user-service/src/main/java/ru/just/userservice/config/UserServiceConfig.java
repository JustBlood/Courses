package ru.just.userservice.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import ru.just.userservice.security.SecurityService;

import java.time.Duration;

@Configuration
@EnableCaching
public class UserServiceConfig {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORITIES_CLAIM = "authorities";
    @Value("${spring.data.redis.host}")
    private String redisHost;
    @Value("${spring.data.redis.port}")
    private int redisPort;
    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        return new JedisConnectionFactory(new RedisStandaloneConfiguration(redisHost, redisPort));
    }
    @Bean
    public RedisCacheManager cacheManager() {
        return RedisCacheManager.builder(jedisConnectionFactory())
                .withCacheConfiguration("token-validity",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(35)))
                .build();
    }

    @Bean
    public WebClient.Builder webClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().secure(sslContextSpec -> sslContextSpec.sslContext(
                        SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
                ))
        ));
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        final PreAuthenticatedAuthenticationProvider authenticationProvider = getPreAuthenticatedAuthenticationProvider();
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManager manager,
                                                   SecurityService securityService) throws Exception {
        final PreAuthenticatedAuthenticationProvider provider = getPreAuthenticatedAuthenticationProvider();
        final AuthenticationFilter jwtAuthenticationFilter = getAuthenticationFilter(manager, securityService);
        return http
                .authorizeHttpRequests(authorizeHttpRequests ->
                        authorizeHttpRequests
                                .requestMatchers("/api/v1/users/admin/**")
                                    .hasAnyAuthority("ADMIN", "OWNER")
                                .anyRequest().authenticated())
                .authenticationProvider(provider)
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, CsrfFilter.class)
                .build();
    }

    private static PreAuthenticatedAuthenticationProvider getPreAuthenticatedAuthenticationProvider() {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(token -> new User(token.getPrincipal().toString(), "nopassword", token.getAuthorities()));
        return provider;
    }

    private static AuthenticationFilter getAuthenticationFilter(AuthenticationManager manager, SecurityService securityService) {
        AuthenticationFilter jwtAuthenticationFilter = new AuthenticationFilter(manager, request -> {
            var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
                String rawToken = authorization.replace(BEARER_PREFIX, "");
                if (!securityService.isValidToken(rawToken)) {
                    return null;
                }
                final DecodedJWT decodedJWT = JWT.decode(rawToken);
                return new PreAuthenticatedAuthenticationToken(decodedJWT.getSubject(),
                        rawToken,
                        decodedJWT.getClaim(AUTHORITIES_CLAIM)
                                .asList(String.class)
                                .stream()
                                .map(role -> new SimpleGrantedAuthority(role.replace("ROLE_", "")))
                                .toList());
            }
            return null;
        });
        jwtAuthenticationFilter
                .setSuccessHandler((request, response, authentication) -> CsrfFilter.skipRequest(request));
        jwtAuthenticationFilter
                .setFailureHandler((request, response, exception) -> response.sendError(HttpServletResponse.SC_FORBIDDEN));
        return jwtAuthenticationFilter;
    }
}
