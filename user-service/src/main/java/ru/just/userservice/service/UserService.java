package ru.just.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.dtolib.audit.ChangeType;
import ru.just.userservice.audit.UserChangeEvent;
import ru.just.userservice.dto.CreateUserDto;
import ru.just.userservice.dto.UserDto;
import ru.just.userservice.dto.UserStatus;
import ru.just.userservice.repository.UserChangeEventRepository;
import ru.just.userservice.repository.UserRepository;

import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserChangeEventRepository userChangeEventRepository;

    public Optional<UserDto> getUserById(Long userId) {
        return userRepository.findActiveUserById(userId);
    }

    @Transactional
    public UserDto saveUser(CreateUserDto createUserDto) {
        UserDto dto = userRepository.save(createUserDto);
        userChangeEventRepository.save(getCreateUserChangeEvent(dto.getId(), dto.getId()));
        return dto;
    }

    private UserChangeEvent getCreateUserChangeEvent(Long userId, Long authorId) {
        return new UserChangeEvent()
                .withUserId(userId)
                .withAuthorId(authorId)
                .withChangeTime(ZonedDateTime.now())
                .withChangeType(ChangeType.CREATE);
    }

    @Transactional
    public void deleteUser(Long userId) {
        UserDto user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with specified id doesn't exists"));
        userRepository.updateUserStatus(user.getId(), UserStatus.DELETED);
        userChangeEventRepository.save(new UserChangeEvent()
                .withAuthorId(userId)
                .withUserId(userId)
                .withChangeTime(ZonedDateTime.now())
                .withChangeType(ChangeType.DELETE));
    }
}
