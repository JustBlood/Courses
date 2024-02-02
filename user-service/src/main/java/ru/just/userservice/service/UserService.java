package ru.just.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.dtolib.audit.ChangeType;
import ru.just.userservice.audit.UserChangeEvent;
import ru.just.userservice.dto.CreateUserDto;
import ru.just.userservice.dto.UserDto;
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

    public Optional<UserDto> getUserDtoById(Long userId) {
        return isUserExists(userId) ? userRepository.findById(userId) : Optional.empty();
    }

    @Transactional
    public UserDto saveUser(CreateUserDto createUserDto) {
        UserDto dto = userRepository.save(createUserDto);
        // userChangeEventRepository.save(getCreateUserChangeEvent(entity.getId(), entity.getId()));
        return dto;
    }

//    private UserChangeEvent getCreateUserChangeEvent(Long userId, Long authorId) {
//        return new UserChangeEvent()
//                .withUser(new User().withId(userId))
//                .withAuthor(new User().withId(authorId))
//                .withChangeTime(ZonedDateTime.now())
//                .withChangeType(ChangeType.CREATE);
//    }

    public void deleteUser(Long userId) {
        if (!isUserExists(userId)) {
            throw new NoSuchElementException("User with specified id doesn't exists");
        }
        userChangeEventRepository.save(new UserChangeEvent()
                .withAuthorId(userId)
                .withUserId(userId)
                .withChangeTime(ZonedDateTime.now())
                .withChangeType(ChangeType.DELETE));
    }

    public boolean isUserExists(Long userId) {
        return userRepository.existsById(userId)
                && userChangeEventRepository.findByUserIdAndChangeType(userId, ChangeType.DELETE).isEmpty();
    }
}
