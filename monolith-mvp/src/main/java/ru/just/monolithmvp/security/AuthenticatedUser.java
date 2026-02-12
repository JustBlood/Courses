package ru.just.monolithmvp.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.just.monolithmvp.model.Role;

import java.util.Collection;

@Getter
@RequiredArgsConstructor
public class AuthenticatedUser implements UserDetails {
    private final Long id;
    private final String username;
    private final String password;
    private final Role role;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
