package ru.just.securityservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.just.securityservice.config.token.model.Token;
import ru.just.securityservice.config.token.model.TokenUser;
import ru.just.securityservice.model.User;
import ru.just.securityservice.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DefaultAuthenticationUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken authenticationToken) throws UsernameNotFoundException {
        if (authenticationToken.getPrincipal() instanceof Token token) {
            final Optional<User> user = userRepository.findByUsername(token.subject());
            return new TokenUser(token.subject(), "nopassword", true, user.isPresent(),
                    token.expiresAt().isAfter(Instant.now()),
                    true,
                    token.authorities().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList(), token, user.orElse(null));
        }
        throw new UsernameNotFoundException("Principal must be of type Token");
    }
}
