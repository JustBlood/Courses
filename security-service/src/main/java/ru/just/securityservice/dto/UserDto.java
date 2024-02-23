package ru.just.securityservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.just.dtolib.base.Dto;
import ru.just.securityservice.model.Role;
import ru.just.securityservice.model.User;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Accessors(chain = true)
public class UserDto extends Dto<User> {
    private Long userId;
    private String login;
    private String email;
    private List<String> roles;

    @Override
    public UserDto fromEntity(User entity) {
        userId = entity.getUserId();
        login = entity.getLogin();
        email = entity.getEmail();
        roles = entity.getRoles().stream().map(Role::getName).toList();
        return this;
    }

    @Override
    public User toEntity() {
        return new User()
                .setUserId(userId)
                .setLogin(login)
                .setEmail(email)
                .setRoles(roles.stream().map(role -> new Role().setName(role)).collect(Collectors.toSet()));
    }
}
