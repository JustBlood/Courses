package ru.just.securityservice.config.token.model;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import ru.just.securityservice.model.User;

import java.util.Collection;

@Getter
public class TokenUser extends org.springframework.security.core.userdetails.User {
    private final Token token;
    private final User user;
    public TokenUser(String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities, Token token, User user) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.token = token;
        this.user = user;
    }
}
