package ru.just.monolithmvp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.just.monolithmvp.model.PasswordSetupToken;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.repository.PasswordSetupTokenRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:task05-submit-flow-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.profiles.active=h2,mail-noop"
})
class Task05SubmitFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordSetupTokenRepository passwordSetupTokenRepository;

    @Autowired
    private LessonSubmissionRepository lessonSubmissionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void alignH2QuestionProgressColumnForTargetConverterFlow() {
        jdbcTemplate.execute("alter table lesson_submissions alter column question_progress_json varchar");
    }

    @Test
    void practice_test_submit_should_upsert_single_submission_and_block_after_finalization() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("task05-upsert-student");
        Long studentId = createUser(adminToken, "Task05 Upsert Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        Long courseId = createCourse(adminToken, "Task05 Upsert Course");
        enrollStudent(adminToken, courseId, studentId);

        Long lessonId = createPracticeTestLesson(adminToken, courseId, "Task05 Upsert Practice", """
                {
                  "title": "%s",
                  "description": "Single submission upsert",
                  "lessonType": "PRACTICE_TEST",
                  "fullPoints": 2,
                  "passingThresholdPercent": 100,
                  "questions": [
                    {
                      "position": 1,
                      "questionType": "SINGLE_CHOICE",
                      "questionText": "2+2",
                      "options": ["3", "4"],
                      "correctAnswers": ["4"],
                      "fullPoints": 2
                    }
                  ]
                }
                """);

        String firstAttemptResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", lessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["3"]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode firstAttempt = objectMapper.readTree(firstAttemptResponse);
        Long firstSubmissionId = firstAttempt.get("submissionId").asLong();
        assertThat(firstAttempt.get("status").asText()).isEqualTo("INCOMPLETE");
        assertThat(firstAttempt.get("completed").asBoolean()).isFalse();
        assertThat(lessonSubmissionRepository.countByStudentIdAndLessonId(studentId, lessonId)).isEqualTo(1);

        String secondAttemptResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", lessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["4"]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode secondAttempt = objectMapper.readTree(secondAttemptResponse);
        Long secondSubmissionId = secondAttempt.get("submissionId").asLong();
        assertThat(secondSubmissionId).isEqualTo(firstSubmissionId);
        assertThat(secondAttempt.get("status").asText()).isEqualTo("COMPLETE");
        assertThat(secondAttempt.get("completed").asBoolean()).isTrue();
        assertThat(lessonSubmissionRepository.countByStudentIdAndLessonId(studentId, lessonId)).isEqualTo(1);

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", lessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["4"]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void practice_test_multiple_choice_should_apply_strict_partial_formula() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("task05-partial-student");
        Long studentId = createUser(adminToken, "Task05 Partial Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        Long courseId = createCourse(adminToken, "Task05 Partial Course");
        enrollStudent(adminToken, courseId, studentId);

        Long partialLessonId = createPracticeTestLesson(adminToken, courseId, "Task05 Partial Practice", """
                {
                  "title": "%s",
                  "description": "Strict partial formula",
                  "lessonType": "PRACTICE_TEST",
                  "fullPoints": 3,
                  "passingThresholdPercent": 33,
                  "questions": [
                    {
                      "position": 1,
                      "questionType": "MULTIPLE_CHOICE",
                      "questionText": "Select A,B,C",
                      "options": ["A", "B", "C", "D"],
                      "correctAnswers": ["A", "B", "C"],
                      "fullPoints": 3,
                      "partialPoints": 1
                    }
                  ]
                }
                """);

        String partialAttemptResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", partialLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["A", "B", "D"]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode partialAttempt = objectMapper.readTree(partialAttemptResponse);
        assertThat(partialAttempt.get("status").asText()).isEqualTo("COMPLETE");
        assertThat(partialAttempt.get("completed").asBoolean()).isTrue();

        Long zeroLessonId = createPracticeTestLesson(adminToken, courseId, "Task05 Zero Practice", """
                {
                  "title": "%s",
                  "description": "Strict zero formula",
                  "lessonType": "PRACTICE_TEST",
                  "fullPoints": 3,
                  "passingThresholdPercent": 33,
                  "questions": [
                    {
                      "position": 1,
                      "questionType": "MULTIPLE_CHOICE",
                      "questionText": "Select A,B,C",
                      "options": ["A", "B", "C", "D"],
                      "correctAnswers": ["A", "B", "C"],
                      "fullPoints": 3,
                      "partialPoints": 1
                    }
                  ]
                }
                """);

        String zeroAttemptResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", zeroLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["A"]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode zeroAttempt = objectMapper.readTree(zeroAttemptResponse);
        assertThat(zeroAttempt.get("status").asText()).isEqualTo("INCOMPLETE");
        assertThat(zeroAttempt.get("completed").asBoolean()).isFalse();
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

    private Long createUser(String adminToken, String fullName, String email, String role) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "%s",
                                  "email": "%s",
                                  "role": "%s"
                                }
                                """.formatted(fullName, email, role)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String setPasswordAndLogin(Long userId, String email, String password) throws Exception {
        PasswordSetupToken inviteToken = passwordSetupTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(userId))
                .reduce((a, b) -> b)
                .orElseThrow();

        mockMvc.perform(post("/api/v1/auth/set-password?token=" + inviteToken.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "%s"
                                }
                                """.formatted(password)))
                .andExpect(status().isOk());

        return login(email, password);
    }

    private Long createCourse(String adminToken, String title) throws Exception {
        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Task05 course",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 70,
                                  "deadlineDays": 30
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(createCourseResponse).get("id").asLong();
    }

    private void enrollStudent(String adminToken, Long courseId, Long studentId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());
    }

    private Long createPracticeTestLesson(String adminToken, Long courseId, String title, String payloadTemplate) throws Exception {
        String lessonPayload = payloadTemplate.formatted(title);
        String createLessonResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(createLessonResponse).get("id").asLong();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
