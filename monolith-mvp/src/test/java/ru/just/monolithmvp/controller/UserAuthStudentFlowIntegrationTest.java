package ru.just.monolithmvp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.just.monolithmvp.model.GroupType;
import ru.just.monolithmvp.model.LearningGroup;
import ru.just.monolithmvp.config.properties.JwtProperties;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.GroupMembershipRepository;
import ru.just.monolithmvp.repository.LearningGroupRepository;
import ru.just.monolithmvp.model.PasswordSetupToken;
import ru.just.monolithmvp.repository.PasswordSetupTokenRepository;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-flow-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.profiles.active=h2"
})
class UserAuthStudentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordSetupTokenRepository passwordSetupTokenRepository;

    @Autowired
    private LearningGroupRepository learningGroupRepository;

    @Autowired
    private GroupMembershipRepository groupMembershipRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void full_user_auth_student_business_flow_should_work() throws Exception {
        String adminToken = login("admin@local", "admin123");

        // JWT security checks: valid / expired / tampered
        mockMvc.perform(get("/api/v1/student/my/profile")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        String expiredToken = buildExpiredToken("admin@local", 1L, "ADMIN");
        mockMvc.perform(get("/api/v1/student/my/profile")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());

        String tamperedToken = tamperToken(adminToken);
        mockMvc.perform(get("/api/v1/student/my/profile")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());

        // Негативный кейс: валидация при создании пользователя
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Bad Email User",
                                  "email": "not-an-email",
                                  "role": "STUDENT"
                                }
                                """))
                .andExpect(status().isBadRequest());

        // Смена пароля текущего администратора + повторный логин
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "admin123",
                                  "newPassword": "Admin123!"
                                }
                                """))
                .andExpect(status().isOk());

        adminToken = login("admin@local", "Admin123!");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Onboarding course",
                                  "description": "Auto-assignment check",
                                  "authorFullName": "Admin"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long onboardingCourseId = objectMapper.readTree(createCourseResponse).get("id").asLong();

        LearningGroup onboardingGroup = new LearningGroup();
        onboardingGroup.setTitle("Onboarding Group " + UUID.randomUUID().toString().substring(0, 8));
        onboardingGroup.setType(GroupType.GENERAL);
        onboardingGroup = learningGroupRepository.save(onboardingGroup);

        // Создаём второго администратора с ручной установкой пароля
        String createAdmin2Response = mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Second Admin",
                                  "email": "admin2@example.com",
                                  "role": "ADMIN",
                                  "phone": "+79990000002",
                                  "comment": "manual password",
                                  "password": "Adm2Pass!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long admin2Id = objectMapper.readTree(createAdmin2Response).get("id").asLong();

        String admin2Token = login("admin2@example.com", "Adm2Pass!");

        // ADMIN имеет функциональность STUDENT
        mockMvc.perform(get("/api/v1/student/my/profile")
                        .header("Authorization", "Bearer " + admin2Token))
                .andExpect(status().isOk());

        // Создаём STUDENT без пароля (через инвайт/токен)
        String createStudentResponse = mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Student One",
                                  "email": "student1@example.com",
                                  "role": "STUDENT",
                                  "phone": "+79990000003",
                                  "comment": "invite flow",
                                  "groupIds": ["%s"],
                                  "courseIds": [%d]
                                }
                                """.formatted(onboardingGroup.getId(), onboardingCourseId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long studentId = objectMapper.readTree(createStudentResponse).get("id").asLong();

        assertThat(groupMembershipRepository.existsByGroupIdAndUserId(onboardingGroup.getId(), studentId)).isTrue();
        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, onboardingCourseId)).isTrue();

        PasswordSetupToken inviteToken = passwordSetupTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(studentId))
                .reduce((a, b) -> b)
                .orElseThrow();

        // Установка пароля по токену
        mockMvc.perform(post("/api/v1/auth/set-password?token=" + inviteToken.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "StudPass1!"
                                }
                                """))
                .andExpect(status().isOk());

        // Негативный кейс: повторное использование того же токена
        mockMvc.perform(post("/api/v1/auth/set-password?token=" + inviteToken.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "StudPass1!"
                                }
                                """))
                .andExpect(status().isBadRequest());

        String studentToken = login("student1@example.com", "StudPass1!");

        // Негативный кейс: смена пароля с неверным currentPassword
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "wrong",
                                  "newPassword": "StudPass2!"
                                }
                                """))
                .andExpect(status().isBadRequest());

        // Позитивная смена пароля студента
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "StudPass1!",
                                  "newPassword": "StudPass2!"
                                }
                                """))
                .andExpect(status().isOk());

        // Негативный кейс: воостановление пароля несуществующего студента
        mockMvc.perform(post("/api/v1/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unexpected@unexpected.unexpected"
                                }
                                """))
                .andExpect(status().isNotFound());

        // Позитивное воостановление пароля студента
        mockMvc.perform(post("/api/v1/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student1@example.com"
                                }
                                """))
                .andExpect(status().isOk());

        PasswordSetupToken recoverToken = passwordSetupTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(studentId) && t.getUsedAt() == null)
                .reduce((a, b) -> b)
                .orElseThrow();

        // Позитивный кейс: восстановление пароля по токену
        mockMvc.perform(post("/api/v1/auth/set-password?token=" + recoverToken.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "StudPass3!"
                                }
                                """))
                .andExpect(status().isOk());

        // Негативный кейс: восстановление пароля по использованному токену
        mockMvc.perform(post("/api/v1/auth/set-password?token=" + recoverToken.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "StudPass3!"
                                }
                                """))
                .andExpect(status().isBadRequest());



        studentToken = login("student1@example.com", "StudPass3!");

        MockMultipartFile avatar = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{1, 2, 3, 4, 5}
        );

        String avatarUploadResponse = mockMvc.perform(multipart("/api/v1/student/my/avatar")
                        .file(avatar)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(avatarUploadResponse).get("avatarFilePath").asText())
                .isNotBlank();

        // Student controller
        mockMvc.perform(get("/api/v1/student/my/profile")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/student/my/profile")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Student Self Updated",
                                  "phone": "+79992223344",
                                  "comment": "self updated"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/student/my/profile")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "hacker@example.com",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest());

        // Негативный кейс: студент не может ходить в admin endpoints
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // UsersController: получение, обновление, роль, установка пароля, активация, экспорт, импорт, удаление
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/users/{userId}", studentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/admin/users/{userId}", studentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Student One Updated",
                                  "email": "student1@example.com",
                                  "role": "STUDENT",
                                  "phone": "+79991112233",
                                  "comment": "updated"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/role", studentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk());

        // Роль применяется немедленно: существующий token student получает доступ к admin endpoints
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/{userId}/password", studentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "StudPass3!"
                                }
                                """))
                .andExpect(status().isOk());

        String studentAsAdminToken = login("student1@example.com", "StudPass3!");
        JsonNode studentAsAdminLogin = parseLogin(studentAsAdminToken);
        assertThat(studentAsAdminLogin.get("role").asText()).isEqualTo("ADMIN");

        // deactivate -> login denied
        mockMvc.perform(post("/api/v1/admin/users/activation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activate": false,
                                  "userIds": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        // Старый Bearer-токен после деактивации больше не дает доступ к защищенным endpoint'ам
        mockMvc.perform(get("/api/v1/student/my/profile")
                        .header("Authorization", "Bearer " + studentAsAdminToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student1@example.com",
                                  "password": "StudPass3!"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        // activate back
        mockMvc.perform(post("/api/v1/admin/users/activation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activate": true,
                                  "userIds": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/users/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        String csvLine = "student;imported1@example.com;;Imported User;;;;;;Company A;;Dept A;;Position A;Imported from CSV;;;;;01.01.2026 10:00;System Admin;;;;\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                csvLine.getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/users/import")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d, %d]
                                }
                                """.formatted(studentId, admin2Id)))
                .andExpect(status().isOk());
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

    private JsonNode parseLogin(String token) throws Exception {
        String payload = token.substring(token.indexOf('.') + 1, token.lastIndexOf('.'));
        String json = new String(java.util.Base64.getUrlDecoder().decode(payload));
        return objectMapper.readTree(json);
    }

    private String buildExpiredToken(String subject, Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expired = new Date(now.getTime() - 60_000);
        Date issuedAt = new Date(now.getTime() - 120_000);

        return Jwts.builder()
                .subject(subject)
                .claim("uid", userId)
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiration(expired)
                .signWith(key)
                .compact();
    }

    private String tamperToken(String token) {
        char replacement = token.charAt(token.length() - 1) == 'a' ? 'b' : 'a';
        return token.substring(0, token.length() - 1) + replacement;
    }
}
