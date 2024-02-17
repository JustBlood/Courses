package ru.just.securityservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.securityservice.dto.CreateUserDto;
import ru.just.securityservice.model.Role;
import ru.just.securityservice.repository.RoleRepository;
import ru.just.securityservice.repository.UserRepository;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public void register(CreateUserDto createUserDto) {
        Role role = roleRepository.findByNameEndsWith("USER")
                .orElseThrow(() -> new NoSuchElementException("Can't register user. Internal security error"));
        createUserDto.getRoles().add(role);
        userRepository.save(createUserDto.toEntity());
    }
}
