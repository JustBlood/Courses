package ru.just.userservice.controller;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import ru.just.userservice.config.PostgresqlContainer;
import ru.just.userservice.dto.CreateUserDto;
import ru.just.userservice.dto.UserDto;
import ru.just.userservice.repository.UserRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UserControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    String host = "http://localhost:%d";

    static PostgresqlContainer postgresqlContainer = PostgresqlContainer.getInstance();

    static {
        postgresqlContainer.start();
    }

    @BeforeEach
    void init() {
        host = String.format(host, port);
    }

    @Test
    @SneakyThrows
    void getUserById() {
        final CreateUserDto createUserDto = new CreateUserDto("user_test", "firstName",
                "lastName", "+79787233623", LocalDate.now()
        );
        long userId = userRepository.save(createUserDto).getId();

        var userDtoResponseEntity = restTemplate.getForEntity(
                host + "/api/v1/internal/users/admin/" + userId, UserDto.class);

        assertEquals(HttpStatus.OK, userDtoResponseEntity.getStatusCode());
        final UserDto body = userDtoResponseEntity.getBody();
        assertNotNull(body);
        assertEquals(createUserDto.getUsername(), body.getUsername());
        assertEquals(createUserDto.getFirstName(), body.getFirstName());
        assertEquals(createUserDto.getLastName(), body.getLastName());
        assertEquals(createUserDto.getPhone(), body.getPhone());
        assertEquals(createUserDto.getRegistrationDate(), body.getRegistrationDate());
    }

    @Test
    @SneakyThrows
    void getUserById_userDoesntExists_notFound() {
        final CreateUserDto createUserDto = new CreateUserDto("user_test", "firstName",
                "lastName", "+79787233623", LocalDate.now()
        );
        long userId = userRepository.save(createUserDto).getId() + 1;

        var userDtoResponseEntity = restTemplate.getForEntity(
                host + "/api/v1/internal/users/admin/" + userId, UserDto.class);

        assertEquals(HttpStatus.NOT_FOUND, userDtoResponseEntity.getStatusCode());
    }

    @Test
    void saveUser() {
        final CreateUserDto createUserDto = new CreateUserDto("user_test", "firstName",
                "lastName", "+79787233623", LocalDate.now()
        );
        RequestEntity<CreateUserDto> request = RequestEntity.post(host + "/api/v1/internal/users/admin")
                        .body(createUserDto);

        var userDtoResponseEntity = restTemplate.exchange(request, UserDto.class);

        assertEquals(HttpStatus.CREATED, userDtoResponseEntity.getStatusCode());
        final UserDto body = userDtoResponseEntity.getBody();
        assertNotNull(body);
        assertNotNull(body.getId());
        assertEquals(createUserDto.getUsername(), body.getUsername());
        assertEquals(createUserDto.getFirstName(), body.getFirstName());
        assertEquals(createUserDto.getLastName(), body.getLastName());
        assertEquals(createUserDto.getPhone(), body.getPhone());
        assertEquals(createUserDto.getRegistrationDate(), body.getRegistrationDate());
    }

    @Test
    void saveUser_incorrectFields_badRequest() {
        final CreateUserDto createUserDto = new CreateUserDto("user_test", "firstName",
                "lastName", "+79787233623", LocalDate.now()
        );

        createUserDto.setUsername(null);
        RequestEntity<CreateUserDto> request = RequestEntity.post(host + "/api/v1/internal/users/admin")
                        .body(createUserDto);
        var userDtoResponseEntity = restTemplate.exchange(request, UserDto.class);

        createUserDto.setUsername("");
        userDtoResponseEntity = restTemplate.exchange(request, UserDto.class);
        assertEquals(HttpStatus.BAD_REQUEST, userDtoResponseEntity.getStatusCode());

        createUserDto.setUsername("user_test");
        createUserDto.setLastName(null);
        userDtoResponseEntity = restTemplate.exchange(request, UserDto.class);
        assertEquals(HttpStatus.BAD_REQUEST, userDtoResponseEntity.getStatusCode());

        createUserDto.setLastName("");
        userDtoResponseEntity = restTemplate.exchange(request, UserDto.class);
        assertEquals(HttpStatus.BAD_REQUEST, userDtoResponseEntity.getStatusCode());

        createUserDto.setUsername("lastName");
        createUserDto.setFirstName(null);
        userDtoResponseEntity = restTemplate.exchange(request, UserDto.class);
        assertEquals(HttpStatus.BAD_REQUEST, userDtoResponseEntity.getStatusCode());

        createUserDto.setFirstName("");
        userDtoResponseEntity = restTemplate.exchange(request, UserDto.class);
        assertEquals(HttpStatus.BAD_REQUEST, userDtoResponseEntity.getStatusCode());

        createUserDto.setFirstName("firstName");
        createUserDto.setPhone("phone");
        userDtoResponseEntity = restTemplate.exchange(request, UserDto.class);
        assertEquals(HttpStatus.BAD_REQUEST, userDtoResponseEntity.getStatusCode());
    }

    @Test
    void updateUser() {
    }

    @Test
    void deleteUser() {
    }
}
