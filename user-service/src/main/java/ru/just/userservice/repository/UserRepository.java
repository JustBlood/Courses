package ru.just.userservice.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Repository;
import ru.just.dtolib.kafka.users.UpdateUserAction;
import ru.just.dtolib.kafka.users.UserAction;
import ru.just.model.tables.records.UsersRecord;
import ru.just.userservice.dto.CreateUserDto;
import ru.just.userservice.dto.UpdateUserDto;
import ru.just.userservice.dto.UserDto;
import ru.just.userservice.dto.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ru.just.model.tables.Users.USERS;

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
                .withPhotoUrl(record.get(USERS.AVATAR_FILE_ID))
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

    public void updateUserById(Long userId, UpdateUserAction userAction) {
        jooq.update(USERS)
                .set(USERS.FIRST_NAME, userAction.getFirstName())
                .set(USERS.LAST_NAME, userAction.getLastName())
                .set(USERS.PHONE, userAction.getPhone())
                .where(USERS.USER_ID.eq(userId)).execute();
    }

    public void saveUserPhoto(Long userId, UUID avatarId) {
        jooq.update(USERS)
                .set(USERS.AVATAR_FILE_ID, avatarId.toString())
                .where(USERS.USER_ID.eq(userId)).execute();
    }

    public List<UserDto> findAllByIds(List<Long> usersInfoByIdsDto) {
        final Result<UsersRecord> records = jooq.selectFrom(USERS)
                .where(USERS.USER_ID.in(usersInfoByIdsDto))
                .fetch();
        return records.stream().map(this::mapUserToDto).toList();
    }
}
