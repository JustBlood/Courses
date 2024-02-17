package ru.just.securityservice.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.just.securityservice.model.User;
import ru.just.securityservice.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DefaultAuthenticationUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private final UserRepository userRepository;
    private final SecurityService securityService;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken authenticationToken) throws UsernameNotFoundException {
        if (authenticationToken.getPrincipal() instanceof DecodedJWT token) {
            final Optional<User> user = userRepository.findById(Long.parseLong(token.getSubject()));
            return new org.springframework.security.core.userdetails.User(
                    user.orElse(new User().withUsername("")).getUsername(),
                    "",
                    true,
                    user.isPresent(),
                    token.getExpiresAtAsInstant().isAfter(Instant.now()),
                    true,
                    securityService.getAuthoritiesFromToken(token));
        }
        throw new UsernameNotFoundException("Principal must be of type Token");
    }
}
