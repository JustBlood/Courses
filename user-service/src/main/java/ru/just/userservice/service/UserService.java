package ru.just.userservice.service;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.dtolib.audit.ChangeType;
import ru.just.dtolib.kafka.users.UserAction;
import ru.just.userservice.audit.UserChangeEvent;
import ru.just.userservice.dto.UpdateUserDto;
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
//    @Value("${topics.user-actions-topic}")
//    private String userActionsTopic;

    public Optional<UserDto> getUserById(Long userId) {
        return userRepository.findActiveUserById(userId);
    }

    @Transactional
    public UserDto updateUser(UpdateUserDto updateUserDto) {
        UserDto dto = userRepository.save(updateUserDto);
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

    @KafkaListener(topics = {"${topics.user-actions-topic}"})
    public void handleUserAction(ConsumerRecord<String, UserAction> userActionRecord) {
        final UserAction userAction = userActionRecord.value();
        if (userAction.getUserActionType().equals(UserAction.UserActionType.CREATED)) {
            userRepository.save(userAction);
        } else if (userAction.getUserActionType().equals(UserAction.UserActionType.DELETED)) {
            userRepository.deleteById(userAction.getUserId());
        }
    }
}
