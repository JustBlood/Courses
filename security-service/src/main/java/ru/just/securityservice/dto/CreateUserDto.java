package ru.just.securityservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.dtolib.base.Dto;
import ru.just.securityservice.model.Role;
import ru.just.securityservice.model.User;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class CreateUserDto extends Dto<User> {
    private String password;
    private String username;
    private String email;
    private Set<Role> roles = new HashSet<>();

    @Override
    public Dto<User> fromEntity(User entity) {
        this.username = entity.getUsername();
        this.password = entity.getPassword();
        this.email = entity.getEmail();
        this.roles = entity.getRoles();
        return this;
    }

    @Override
    public User toEntity() {
        return new User()
                .withUsername(username)
                .withPassword(password)
                .withEmail(email)
                .withRoles(roles);
    }
}
