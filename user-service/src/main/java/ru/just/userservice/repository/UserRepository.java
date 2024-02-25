package ru.just.userservice.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import ru.just.dtolib.kafka.users.UserAction;
import ru.just.userservice.dto.CreateUserDto;
import ru.just.userservice.dto.UpdateUserDto;
import ru.just.userservice.dto.UserDto;
import ru.just.userservice.dto.UserStatus;

import java.util.Optional;

import static org.jooq.generated.public_.tables.Users.USERS;

@RequiredArgsConstructor
@Repository
public class UserRepository {
    private final DSLContext jooq;

    public Optional<UserDto> findActiveUserById(Long userId) {
        final Record record = jooq.select().from(USERS).where(
                USERS.USER_ID.eq(userId).and(USERS.STATUS.eq(UserStatus.ACTIVE.name())))
                .fetchOne();
        return Optional.ofNullable(mapUserToDto(record));
    }

    private UserDto mapUserToDto(Record record) {
        if (record == null || record.get(USERS.STATUS).equalsIgnoreCase(UserStatus.DELETED.name())) {
            return null;
        }
        return new UserDto()
                .withId(record.get(USERS.USER_ID))
                .withUsername(record.get(USERS.USERNAME))
                .withFirstName(record.get(USERS.FIRST_NAME))
                .withLastName(record.get(USERS.LAST_NAME))
                .withPhone(record.get(USERS.PHONE))
                .withUserStatus(Enum.valueOf(UserStatus.class, record.get(USERS.STATUS)))
                .withRegistrationDate(record.get(USERS.REGISTRATION_DATE));
    }

    public UserDto save(CreateUserDto updateUserDto) {
        final Record record = jooq.insertInto(USERS)
                .set(USERS.USERNAME, updateUserDto.getUsername())
                .set(USERS.FIRST_NAME, updateUserDto.getFirstName())
                .set(USERS.LAST_NAME, updateUserDto.getLastName())
                .set(USERS.PHONE, updateUserDto.getPhone())
                .set(USERS.STATUS, UserStatus.ACTIVE.name())
                .set(USERS.REGISTRATION_DATE, updateUserDto.getRegistrationDate())
                .returning().fetchOne();
        return mapUserToDto(record);
    }

    public UserDto updateUserStatus(Long userId, UserStatus userStatus) {
        final Record record = jooq.update(USERS)
                .set(USERS.STATUS, userStatus.name())
                .where(USERS.USER_ID.eq(userId))
                .returning().fetchOne();
        return mapUserToDto(record);
    }

    public UserDto saveUserFromSecurityService(UserAction userAction) {
        final Record record = jooq.insertInto(USERS)
                .set(USERS.USER_ID, userAction.getUserId())
                .set(USERS.USERNAME, userAction.getLogin())
                .returning().fetchOne();
        return mapUserToDto(record);
    }

    public void deleteById(Long id) {
        jooq.delete(USERS).where(USERS.USER_ID.eq(id))
                .execute();
    }

    public void updateUserById(Long userId, UpdateUserDto userDto) {
        jooq.update(USERS)
                .set(USERS.FIRST_NAME, userDto.getFirstName())
                .set(USERS.LAST_NAME, userDto.getLastName())
                .set(USERS.USERNAME, userDto.getUsername())
                .set(USERS.PHONE, userDto.getPhone())
                .where(USERS.USER_ID.eq(userId)).execute();
    }
}
