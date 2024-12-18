package ru.just.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.just.dtolib.audit.ChangeType;
import ru.just.dtolib.response.media.FileUrlDto;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserChangeEventRepository userChangeEventRepository;
    private final MediaService mediaService;

    public Optional<UserDto> getUserById(Long userId) {
        final Optional<UserDto> activeUserById = userRepository.findActiveUserById(userId);
        return activeUserById.map(user -> {
            final UUID fileId = UUID.fromString(user.getPhotoUrl());
            String photoUrl = mediaService.getAvatar(List.of(fileId)).get(fileId).getUrl();
            user.setPhotoUrl(photoUrl);
            return user;
        });
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

    public void addAvatarToUser(Long userId, MultipartFile file) {
        UUID fileId = mediaService.uploadAvatarPhoto(file).getFileId();
        userRepository.saveUserPhoto(userId, fileId);
    }

    public List<UserDto> getUsersByIds(List<Long> usersInfoByIdsDto) {
        final List<UserDto> users = userRepository.findAllByIds(usersInfoByIdsDto);
        final List<UUID> fileIdDtos = users.stream()
                .map(UserDto::getPhotoUrl)
                .filter(StringUtils::isNotBlank)
                .map(UUID::fromString)
                .toList();
        final Map<UUID, FileUrlDto> avatarMap = mediaService.getAvatar(fileIdDtos);
        return users.stream()
                .peek(user -> user.setPhotoUrl(avatarMap.get(UUID.fromString(user.getPhotoUrl())).getUrl()))
                .collect(Collectors.toList());
    }
}
