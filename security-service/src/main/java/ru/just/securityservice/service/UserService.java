package ru.just.securityservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.securityservice.dto.CreateUserDto;
import ru.just.securityservice.dto.UserDto;
import ru.just.securityservice.model.Role;
import ru.just.securityservice.repository.RoleRepository;
import ru.just.securityservice.repository.UserRepository;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class UserService {
    public static final String STUDENT_ROLE = "STUDENT";
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto register(CreateUserDto createUserDto) {
        Role role = roleRepository.findByNameEndsWith(STUDENT_ROLE)
                .orElseThrow(() -> new NoSuchElementException("Can't register user. Internal security error"));
        createUserDto.setPassword(passwordEncoder.encode(createUserDto.getPassword()));
        createUserDto.getRoles().add(role);
        return new UserDto().fromEntity(userRepository.save(createUserDto.toEntity()));
    }
}
