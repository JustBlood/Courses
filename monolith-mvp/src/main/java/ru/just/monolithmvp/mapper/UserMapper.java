package ru.just.monolithmvp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.just.monolithmvp.dto.group.GroupDto;
import ru.just.monolithmvp.dto.user.UserDto;
import ru.just.monolithmvp.model.AppUser;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "ldtToEpochSec")
    @Mapping(source = "lastVisit", target = "lastVisit", qualifiedByName = "ldtToEpochSec")
    @Mapping(source = "deactivatedAt", target = "deactivatedAt", qualifiedByName = "ldtToEpochSec")
    UserDto toDto(AppUser user);

    @Mapping(source = "user.createdAt", target = "createdAt", qualifiedByName = "ldtToEpochSec")
    @Mapping(source = "user.lastVisit", target = "lastVisit", qualifiedByName = "ldtToEpochSec")
    @Mapping(source = "user.deactivatedAt", target = "deactivatedAt", qualifiedByName = "ldtToEpochSec")
    @Mapping(source = "userGroups", target = "groups")
    UserDto toDto(AppUser user, List<GroupDto> userGroups);

    @Named("ldtToEpochSec")
    default Long ldtToEpochSec(LocalDateTime v) {
        return v == null ? null : v.toEpochSecond(ZoneOffset.UTC);
    }
}
