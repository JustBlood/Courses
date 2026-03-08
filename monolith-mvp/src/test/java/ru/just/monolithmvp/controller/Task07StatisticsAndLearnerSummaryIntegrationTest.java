package ru.just.monolithmvp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.just.monolithmvp.model.PasswordSetupToken;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.repository.PasswordSetupTokenRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:task07-stats-summary-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.profiles.active=h2,mail-noop"
})
class Task07StatisticsAndLearnerSummaryIntegrationTest {

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
    void stats_and_learner_summary_should_use_single_submission_and_attempt_counter() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("task07-student");
        Long studentId = createUser(adminToken, "Task07 Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        Long courseId = createCourse(adminToken, "Task07 Course");
        enrollStudent(adminToken, courseId, studentId);
        Long lessonId = createPracticeTestLesson(adminToken, courseId, "Task07 Practice");

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", lessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["3"]
                                  }
                                }
                                """))
                .andExpect(status().isOk());

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
                .andExpect(status().isOk());

        int attemptCounter = lessonSubmissionRepository.findByStudentIdAndLessonId(studentId, lessonId)
                .orElseThrow()
                .getAttemptCounter();
        assertThat(attemptCounter).isEqualTo(2);

        String learnerCourseResponse = mockMvc.perform(get("/api/v1/student/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode learnerCourse = objectMapper.readTree(learnerCourseResponse);
        JsonNode lessonSummary = learnerCourse.get("lessons").get(0);
        assertThat(lessonSummary.get("passed").asBoolean()).isTrue();
        assertThat(lessonSummary.get("pointsAwarded").asInt()).isEqualTo(2);

        String myStatsResponse = mockMvc.perform(get("/api/v1/student/my/stats")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode myStats = objectMapper.readTree(myStatsResponse);
        assertThat(myStats.size()).isEqualTo(1);
        assertThat(myStats.get(0).get("earnedPoints").asInt()).isEqualTo(2);

        String courseStatsResponse = mockMvc.perform(get("/api/v1/admin/progress/courses/{courseId}/stats", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode courseStats = objectMapper.readTree(courseStatsResponse);
        assertThat(courseStats.size()).isEqualTo(1);
        assertThat(courseStats.get(0).get("earnedPoints").asInt()).isEqualTo(2);

        String courseSummaryCsv = mockMvc.perform(get("/api/v1/admin/progress/courses/{courseId}/summary-report.csv", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String[] lines = courseSummaryCsv.strip().split("\\R");
        assertThat(lines.length).isGreaterThanOrEqualTo(2);
        String[] studentRow = parseCsvSemicolonLine(lines[1]);
        assertThat(studentRow[11]).isEqualTo("2");
        assertThat(studentRow[14]).isEqualTo("1");
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
                                  "description": "Task07 course",
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

    private Long createPracticeTestLesson(String adminToken, Long courseId, String title) throws Exception {
        String createLessonResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Task07 practice",
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
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(createLessonResponse).get("id").asLong();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private String[] parseCsvSemicolonLine(String line) {
        String content = line;
        if (content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);
        }
        return content.split("\";\"", -1);
    }
}
