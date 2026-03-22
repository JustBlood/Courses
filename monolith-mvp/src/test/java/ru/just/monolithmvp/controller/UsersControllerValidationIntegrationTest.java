package ru.just.monolithmvp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:users-validation-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.security.jwt.secret=test-jwt-secret-key-at-least-32-bytes-12345",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.profiles.active=h2"
})
class UsersControllerValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createUser_shouldPersistSnils() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String createResponse = mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "User With Snils",
                                  "email": "snils-user@example.com",
                                  "role": "STUDENT",
                                  "snils": "123-456-789 00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long userId = objectMapper.readTree(createResponse).get("id").asLong();
        assertThat(objectMapper.readTree(createResponse).get("snils").asText()).isEqualTo("123-456-789 00");

        String getResponse = mockMvc.perform(get("/api/v1/admin/users/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(getResponse).get("snils").asText()).isEqualTo("123-456-789 00");
    }

    @Test
    void updateUser_shouldReturnBadRequest_forInvalidEmail() throws Exception {
        String adminToken = login("admin@local", "admin123");

        mockMvc.perform(put("/api/v1/admin/users/{userId}", 1L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAvailableToAssignUsers_shouldReturnAllUsers_forEmptyGeneralGroup() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long userAId = createUser(adminToken, "available-a-" + UUID.randomUUID() + "@example.com");
        Long userBId = createUser(adminToken, "available-b-" + UUID.randomUUID() + "@example.com");
        UUID groupId = createGroup(adminToken, "Empty General " + UUID.randomUUID());

        String response = mockMvc.perform(get("/api/v1/admin/groups/{groupId}/users/availableToAssign", groupId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var users = objectMapper.readTree(response);
        assertThat(users.isArray()).isTrue();
        assertThat(users).isNotEmpty();
        assertThat(containsUserId(users, userAId)).isTrue();
        assertThat(containsUserId(users, userBId)).isTrue();
    }

    private Long createUser(String adminToken, String email) throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Available User",
                                  "email": "%s",
                                  "role": "STUDENT",
                                  "password": "Password1!"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(createResponse).get("id").asLong();
    }

    private UUID createGroup(String adminToken, String title) throws Exception {
        String createGroupResponse = mockMvc.perform(post("/api/v1/admin/groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "type": "GENERAL"
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(objectMapper.readTree(createGroupResponse).get("id").asText());
    }

    private boolean containsUserId(com.fasterxml.jackson.databind.JsonNode users, Long userId) {
        if (users == null || !users.isArray()) {
            return false;
        }
        for (var user : users) {
            if (user.has("id") && user.get("id").asLong() == userId) {
                return true;
            }
        }
        return false;
    }

    private String login(String email, String password) throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(loginResponse).get("token").asText();
    }
}
