package ru.just.courses.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.courses.dto.CreateUserDto;
import ru.just.courses.dto.UserDto;
import ru.just.courses.model.audit.ChangeType;
import ru.just.courses.model.audit.UserChangeEvent;
import ru.just.courses.model.user.User;
import ru.just.courses.repository.UserChangeEventRepository;
import ru.just.courses.repository.UserRepository;

import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserChangeEventRepository userChangeEventRepository;

    public Optional<UserDto> getUserDtoById(Long userId) {
        return isUserExists(userId) ? userRepository.findById(userId).map(new UserDto()::fromEntity) : Optional.empty();
    }

    @Transactional
    public UserDto saveUser(CreateUserDto createUserDto) {
        final User entity = userRepository.save(createUserDto.toEntity());
        userChangeEventRepository.save(getCreateUserChangeEvent(entity.getId(), entity.getId()));
        return new UserDto().fromEntity(entity);
    }

    private UserChangeEvent getCreateUserChangeEvent(Long userId, Long authorId) {
        return new UserChangeEvent()
                .withUser(new User().withId(userId))
                .withAuthor(new User().withId(authorId))
                .withChangeTime(ZonedDateTime.now())
                .withChangeType(ChangeType.CREATE);
    }

    public void deleteUser(Long userId) {
        if (!isUserExists(userId)) {
            throw new NoSuchElementException("User with specified id doesn't exists");
        }
        userChangeEventRepository.save(new UserChangeEvent()
                .withAuthor(new User().withId(userId))
                .withUser(new User().withId(userId))
                .withChangeTime(ZonedDateTime.now())
                .withChangeType(ChangeType.DELETE));
    }

    public boolean isUserExists(Long userId) {
        return userRepository.existsById(userId)
                && userChangeEventRepository.findByUserIdAndChangeType(userId, ChangeType.DELETE).isEmpty();
    }
}
