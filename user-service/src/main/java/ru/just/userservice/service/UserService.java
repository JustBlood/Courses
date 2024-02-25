package ru.just.userservice.service;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.dtolib.audit.ChangeType;
import ru.just.dtolib.kafka.users.UserAction;
import ru.just.userservice.audit.UserChangeEvent;
import ru.just.userservice.dto.CreateUserDto;
import ru.just.userservice.dto.UpdateUserDto;
import ru.just.userservice.dto.UserDto;
import ru.just.userservice.dto.UserStatus;
import ru.just.userservice.repository.UserChangeEventRepository;
import ru.just.userservice.repository.UserRepository;

import java.time.LocalDateTime;
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
    public UserDto createUser(CreateUserDto createUserDto) {
        UserDto dto = userRepository.save(createUserDto);
        saveUserChangeEvent(dto.getId(), dto.getId(), ChangeType.CREATE);
        return dto;
    }

    private void saveUserChangeEvent(Long userId, Long authorId, ChangeType changeType) {
        userChangeEventRepository.save(new UserChangeEvent()
                .withUserId(userId)
                .withAuthorId(authorId)
                .withChangeTime(LocalDateTime.now())
                .withChangeType(changeType));
    }

    @Transactional
    public void deleteUser(Long userId) {
        final UserDto user = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with specified id doesn't exists"));
        userRepository.updateUserStatus(user.getId(), UserStatus.DELETED);
        saveUserChangeEvent(userId, userId, ChangeType.DELETE);
    }

    @Transactional
    @KafkaListener(topics = {"${topics.user-actions-topic}"})
    public void handleUserAction(ConsumerRecord<String, UserAction> userActionRecord) {
        final UserAction userAction = userActionRecord.value();
        ChangeType changeType = userAction.getUserActionType().equals(UserAction.UserActionType.CREATED) ?
                ChangeType.CREATE : ChangeType.DELETE;
        if (userAction.getUserActionType().equals(UserAction.UserActionType.CREATED)) {
            userRepository.saveUserFromSecurityService(userAction);
        } else if (userAction.getUserActionType().equals(UserAction.UserActionType.DELETED)) {
            userRepository.deleteById(userAction.getUserId());
        }
        saveUserChangeEvent(userAction.getUserId(), userAction.getUserId(), changeType);
    }

    public void updateUser(Long userId, UpdateUserDto userDto) {
        userRepository.updateUserById(userId, userDto);
        saveUserChangeEvent(userId, userId, ChangeType.UPDATE);
    }
}
