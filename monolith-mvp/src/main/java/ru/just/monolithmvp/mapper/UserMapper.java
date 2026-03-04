package ru.just.monolithmvp.mapper;

import org.mapstruct.Mapper;
import ru.just.monolithmvp.dto.user.UserDto;
import ru.just.monolithmvp.model.AppUser;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(AppUser user);
}
