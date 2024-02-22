package ru.just.securityservice.security.userdetails;

import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.just.securityservice.model.User;
import ru.just.securityservice.service.SecurityService;

import java.time.Instant;
import java.util.Collection;

@Getter
public class TokenUser implements UserDetails {
    private final User user;
    private final DecodedJWT token;
    private final boolean isUserEnabled;
    private final boolean isUserNonLocked;

    public TokenUser(User user, DecodedJWT token, boolean isUserEnabled, boolean isUserNonLocked) {
        this.user = user;
        this.token = token;
        this.isUserEnabled = isUserEnabled;
        this.isUserNonLocked = isUserNonLocked;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return token.getClaim(SecurityService.AUTHORITIES_CLAIM).asList(String.class)
                .stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getLogin();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isUserNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return token.getExpiresAtAsInstant().isAfter(Instant.now());
    }

    @Override
    public boolean isEnabled() {
        return isUserEnabled;
    }
}
