package ru.just.securityservice.security.userdetails;

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

@Component
@RequiredArgsConstructor
public class DefaultAuthenticationUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken authenticationToken) throws UsernameNotFoundException {
        if (authenticationToken.getPrincipal() instanceof DecodedJWT token) {
            final User user = userRepository.findById(Long.parseLong(token.getSubject()))
                    .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
            return new TokenUser(user, token, true, true);
        }
        throw new UsernameNotFoundException("Principal must be of type Token");
    }
}
