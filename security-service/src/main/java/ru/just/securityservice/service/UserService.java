package ru.just.securityservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.dtolib.kafka.users.UserAction;
import ru.just.securityservice.controller.exception.UsernameAlreadyExistsException;
import ru.just.securityservice.dto.CreateUserDto;
import ru.just.securityservice.dto.UserDto;
import ru.just.securityservice.model.Role;
import ru.just.securityservice.repository.RoleRepository;
import ru.just.securityservice.repository.UserRepository;

import java.util.NoSuchElementException;

import static ru.just.dtolib.kafka.users.UserAction.UserActionType.CREATED;
import static ru.just.dtolib.kafka.users.UserDeliverStatus.DELIVERED;
import static ru.just.dtolib.kafka.users.UserDeliverStatus.NOT_SENT;

@RequiredArgsConstructor
@Service
public class UserService {
    public static final String STUDENT_ROLE = "STUDENT";
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${topics.user-actions-topic}")
    private String userActionsTopic;

    @Transactional
    public UserDto register(CreateUserDto createUserDto) {
        if (userRepository.existsByLoginOrEmail(createUserDto.getLogin(), createUserDto.getEmail())) {
            throw new UsernameAlreadyExistsException("Login or email already used");
        }
        Role role = roleRepository.findByNameEndsWith(STUDENT_ROLE)
                .orElseThrow(() -> new NoSuchElementException("Can't register user. Internal security error"));
        return saveUser(createUserDto, role);
    }

    private UserDto saveUser(CreateUserDto createUserDto, Role role) {
        createUserDto.setPassword(passwordEncoder.encode(createUserDto.getPassword()));
        createUserDto.getRoles().add(role);
        return new UserDto().fromEntity(userRepository.save(createUserDto.toEntity()));
    }

    public void sendUserToUsersService(UserDto userDto) {
        UserAction createUserAction = UserAction.builder()
                .userId(userDto.getUserId())
                .login(userDto.getLogin())
                .userActionType(CREATED)
                .build();
        kafkaTemplate.send(userActionsTopic, createUserAction)
                .exceptionally(ex -> {
                    userRepository.findById(userDto.getUserId())
                            .ifPresent(user -> userRepository.save(user.setDeliverStatus(NOT_SENT)));
                    return null;
                })
                .thenAcceptAsync(result -> userRepository.findById(userDto.getUserId())
                        .ifPresent(user -> userRepository.save(user.setDeliverStatus(DELIVERED))));
    }
}
