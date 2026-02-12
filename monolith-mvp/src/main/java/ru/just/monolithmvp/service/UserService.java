package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.user.CreateUserRequest;
import ru.just.monolithmvp.dto.user.UserDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.mapper.UserMapper;
import ru.just.monolithmvp.model.AppUser;
import ru.just.monolithmvp.repository.AppUserRepository;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AppUserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonSubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already exists");
        }

        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setEnabled(true);

        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(userMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return userMapper.toDto(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        submissionRepository.deleteByStudentId(userId);
        enrollmentRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }
}
