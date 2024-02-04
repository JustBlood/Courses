package ru.just.securityservice.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import ru.just.dtolib.base.Dto;
import ru.just.securityservice.model.User;

@Getter
@Setter
@Builder
public class UserDto extends Dto<User> {
    private String password;
    private String username;
    private String email;

    @Override
    public Dto<User> fromEntity(User entity) {
        return null;
    }

    @Override
    public User toEntity() {
        return null;
    }
}
