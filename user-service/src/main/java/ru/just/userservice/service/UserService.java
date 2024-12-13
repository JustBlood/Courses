package ru.just.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.just.dtolib.audit.ChangeType;
import ru.just.userservice.audit.UserChangeEvent;
import ru.just.userservice.dto.CreateUserDto;
import ru.just.userservice.dto.UpdateUserDto;
import ru.just.userservice.dto.UserDto;
import ru.just.userservice.dto.UserStatus;
import ru.just.userservice.exception.EntityNotFoundException;
import ru.just.userservice.repository.UserChangeEventRepository;
import ru.just.userservice.repository.UserRepository;
import ru.just.userservice.service.integration.MediaService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserChangeEventRepository userChangeEventRepository;
    private final MediaService mediaService;

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
                .orElseThrow(() -> new EntityNotFoundException("User with specified id doesn't exists"));
        userRepository.updateUserStatus(user.getId(), UserStatus.DELETED);
        saveUserChangeEvent(userId, userId, ChangeType.DELETE);
    }

    @Transactional
    public void updateUser(Long userId, UpdateUserDto userDto) {
        userRepository.updateUserById(userId, userDto);
        saveUserChangeEvent(userId, userId, ChangeType.UPDATE);
    }

    public void addAvatarToUser(Long userId, MultipartFile avatar) {
        UUID avatarUrl = mediaService.uploadAvatarPhoto(avatar).getFileId();
        userRepository.saveUserPhoto(userId, avatarUrl);
    }

    public List<UserDto> getUsersByIds(List<Long> usersInfoByIdsDto) {
        return userRepository.findAllByIds(usersInfoByIdsDto);
    }
}
