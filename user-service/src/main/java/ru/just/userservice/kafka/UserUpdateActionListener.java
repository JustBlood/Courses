package ru.just.userservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.just.dtolib.audit.ChangeType;
import ru.just.dtolib.kafka.users.UpdateUserAction;
import ru.just.userservice.audit.UserChangeEvent;
import ru.just.userservice.repository.UserChangeEventRepository;
import ru.just.userservice.repository.UserRepository;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(topics = {"${topics.user-update-topic}"})
public class UserUpdateActionListener {
    private final UserRepository userRepository;
    private final UserChangeEventRepository userChangeEventRepository;

    @KafkaHandler
    @Transactional
    public void handleUpdateUserAction(UpdateUserAction updateUserAction) {
        userRepository.updateUserById(updateUserAction.getId(), updateUserAction);
        userChangeEventRepository.save(new UserChangeEvent()
                .withUserId(updateUserAction.getId())
                .withAuthorId(updateUserAction.getId())
                .withChangeTime(LocalDateTime.now())
                .withChangeType(ChangeType.UPDATE));
    }
}
