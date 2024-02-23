package ru.just.securityservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.dtolib.base.Dto;
import ru.just.dtolib.kafka.users.UserDeliverStatus;
import ru.just.securityservice.model.Role;
import ru.just.securityservice.model.User;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class CreateUserDto extends Dto<User> {
    private String login;
    private String password;
    private String email;
    @JsonIgnore
    private Set<Role> roles = new HashSet<>();
    @JsonIgnore
    private UserDeliverStatus deliverStatus = UserDeliverStatus.SENT;

    @Override
    public Dto<User> fromEntity(User entity) {
        this.login = entity.getLogin();
        this.password = entity.getPassword();
        this.email = entity.getEmail();
        this.roles = entity.getRoles();
        this.deliverStatus = entity.getUserDeliverStatus();
        return this;
    }

    @Override
    public User toEntity() {
        return new User()
                .setLogin(login)
                .setPassword(password)
                .setEmail(email)
                .setRoles(roles)
                .setUserDeliverStatus(deliverStatus);
    }
}
