package ru.just.userservice.service;

import jakarta.servlet.ServletInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.dtolib.audit.ChangeType;
import ru.just.dtolib.kafka.users.UpdateUserAction;
import ru.just.dtolib.kafka.users.UserAction;
import ru.just.dtolib.users.UsersInfoByIdsDto;
import ru.just.securitylib.service.ThreadLocalTokenService;
import ru.just.userservice.audit.UserChangeEvent;
import ru.just.userservice.dto.CreateUserDto;
import ru.just.userservice.dto.UpdateUserDto;
import ru.just.userservice.dto.UserDto;
import ru.just.userservice.dto.UserStatus;
import ru.just.userservice.repository.UserChangeEventRepository;
import ru.just.userservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserChangeEventRepository userChangeEventRepository;
    private final ThreadLocalTokenService tokenService;

    public Optional<UserDto> getUserByIdFromToken() {
        return getUserById(tokenService.getUserId());
    }

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
    // todo: вынести в kafka-worker какой-нибудь
    @Transactional
    @KafkaListener(topics = {"${topics.user-actions-topic}"})
    public void handleUserAction(ConsumerRecord<String, UserAction> userActionRecord) {
        log.info("Получено сообщение из user-actions топика");
        final UserAction userAction = userActionRecord.value();
        ChangeType changeType = userAction.getUserActionType().equals(UserAction.UserActionType.CREATED) ?
                ChangeType.CREATE : ChangeType.DELETE;
        if (userAction.getUserActionType().equals(UserAction.UserActionType.CREATED)) {
            log.info("Создание нового пользователя");
            userRepository.saveUserFromSecurityService(userAction);
        } else if (userAction.getUserActionType().equals(UserAction.UserActionType.DELETED)) {
            log.info("Удаление пользователя");
            userRepository.deleteById(userAction.getUserId());
        }
        saveUserChangeEvent(userAction.getUserId(), userAction.getUserId(), changeType);
    }
    // todo: вынести в kafka-worker какой-нибудь
    @Transactional
    @KafkaListener(topics = {"${topics.user-update-topic}"})
    public void handleUpdateUserAction(ConsumerRecord<String, UpdateUserAction> consumerRecord) {
        final UpdateUserAction updateUserAction = consumerRecord.value();
        userRepository.updateUserById(updateUserAction.getId(), updateUserAction);
        saveUserChangeEvent(updateUserAction.getId(), updateUserAction.getId(), ChangeType.UPDATE);
    }

    public void updateUser(UpdateUserDto userDto) {
        updateUser(tokenService.getUserId(), userDto);
    }

    public void updateUser(Long userId, UpdateUserDto userDto) {
        userRepository.updateUserById(userId, userDto);
        saveUserChangeEvent(userId, userId, ChangeType.UPDATE);
    }

    public void addPhotoToUser(ServletInputStream inputStream) {
        addPhotoToUser(tokenService.getUserId(), inputStream);
    }

    public void addPhotoToUser(Long userId, ServletInputStream inputStream) {
        // TODO: send to media-service logic
        String photoUrl = "";

        userRepository.saveUserPhoto(userId, photoUrl);
    }

    public List<UserDto> getUsersByIds(UsersInfoByIdsDto usersInfoByIdsDto) {
        return userRepository.findAllByIds(usersInfoByIdsDto);
    }
}
