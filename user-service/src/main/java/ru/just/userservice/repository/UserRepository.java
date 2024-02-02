package ru.just.userservice.repository;

import org.springframework.stereotype.Repository;
import ru.just.userservice.dto.CreateUserDto;
import ru.just.userservice.dto.UserDto;

import java.util.Optional;

@Repository
public class UserRepository {

    public Optional<UserDto> findById(Long userId) {
        return null;
    }

    public UserDto save(CreateUserDto createUserDto) {
        return null;
    }

    public boolean existsById(Long userId) {
        return false;
    }
}
