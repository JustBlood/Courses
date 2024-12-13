package ru.just.userservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.just.dtolib.audit.ChangeType;
import ru.just.dtolib.kafka.users.UserAction;
import ru.just.userservice.audit.UserChangeEvent;
import ru.just.userservice.repository.UserChangeEventRepository;
import ru.just.userservice.repository.UserRepository;
import ru.just.userservice.service.integration.MediaService;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(topics = {"${topics.user-actions-topic}"})
public class UserActionListener {
    private final UserRepository userRepository;
    private final UserChangeEventRepository userChangeEventRepository;
    private final MediaService mediaService;

    @KafkaHandler
    @Transactional
    public void handleUserAction(ConsumerRecord<String, UserAction> userActionRecord) {
        log.info("Получено сообщение из user-actions топика");
        final UserAction userAction = userActionRecord.value();
        ChangeType changeType = userAction.getUserActionType().equals(UserAction.UserActionType.CREATED) ?
                ChangeType.CREATE : ChangeType.DELETE;
        if (userAction.getUserActionType().equals(UserAction.UserActionType.CREATED)) {
            log.info("Создание нового пользователя");
            userRepository.saveUserFromSecurityService(userAction);
            UUID avatarUUID = mediaService.generateAvatar(userAction.getLogin());
            userRepository.saveUserPhoto(userAction.getUserId(), avatarUUID);
        } else if (userAction.getUserActionType().equals(UserAction.UserActionType.DELETED)) {
            log.info("Удаление пользователя");
            userRepository.deleteById(userAction.getUserId());
        }
        userChangeEventRepository.save(new UserChangeEvent()
                .withUserId(userAction.getUserId())
                .withAuthorId(userAction.getUserId())
                .withChangeTime(LocalDateTime.now())
                .withChangeType(changeType));
    }
}
