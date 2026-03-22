package ru.just.monolithmvp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.just.monolithmvp.model.PasswordSetupToken;
import ru.just.monolithmvp.repository.PasswordSetupTokenRepository;

import java.util.Iterator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:task08-learner-visibility-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.profiles.active=h2,mail-noop"
})
class Task08LearnerAnswersVisibilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordSetupTokenRepository passwordSetupTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void alignH2QuestionProgressColumnForTargetConverterFlow() {
        jdbcTemplate.execute("alter table lesson_submissions alter column question_progress_json varchar");
    }

    @Test
    void practice_test_should_show_user_answers_and_correct_answers_only_after_completion() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("task08-practice-student");
        Long studentId = createUser(adminToken, "Task08 Practice Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        Long courseId = createCourse(adminToken, "Task08 Practice Course");
        enrollStudent(adminToken, courseId, studentId);

        Long lessonId = createPracticeTestLesson(adminToken, courseId, "Task08 Practice Lesson", """
                {
                  "title": "%s",
                  "description": "Learner answers visibility",
                  "lessonType": "PRACTICE_TEST",
                  "fullPoints": 2,
                  "passingThresholdPercent": 100,
                  "showQuestionStatus": true,
                  "showCorrectAnswers": true,
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

        JsonNode beforeSubmitQuestion = questionByPosition(getLearnerLesson(studentToken, lessonId), 1);
        assertThat(beforeSubmitQuestion.get("userAnswers").isArray()).isTrue();
        assertThat(beforeSubmitQuestion.get("userAnswers")).isEmpty();
        assertThat(beforeSubmitQuestion.get("correctAnswers").isNull()).isTrue();
        assertThat(beforeSubmitQuestion.get("status").isNull()).isTrue();
        assertThat(beforeSubmitQuestion.get("awardedPoints").isNull()).isTrue();

        submitPractice(studentToken, lessonId, """
                {
                  "questionAnswers": {
                    "1": ["3"]
                  }
                }
                """);

        JsonNode afterFailedSubmitQuestion = questionByPosition(getLearnerLesson(studentToken, lessonId), 1);
        assertThat(afterFailedSubmitQuestion.get("userAnswers").get(0).asText()).isEqualTo("3");
        assertThat(afterFailedSubmitQuestion.get("correctAnswers").isNull()).isTrue();
        assertThat(afterFailedSubmitQuestion.get("status").isNull()).isTrue();
        assertThat(afterFailedSubmitQuestion.get("awardedPoints").asInt()).isZero();

        submitPractice(studentToken, lessonId, """
                {
                  "questionAnswers": {
                    "1": ["4"]
                  }
                }
                """);

        JsonNode afterPassedSubmitQuestion = questionByPosition(getLearnerLesson(studentToken, lessonId), 1);
        assertThat(afterPassedSubmitQuestion.get("userAnswers").get(0).asText()).isEqualTo("4");
        assertThat(afterPassedSubmitQuestion.get("correctAnswers").get(0).asText()).isEqualTo("4");
        assertThat(afterPassedSubmitQuestion.get("status").isNull()).isTrue();
        assertThat(afterPassedSubmitQuestion.get("awardedPoints").asInt()).isEqualTo(2);
    }

    @Test
    void open_lesson_should_show_or_hide_status_and_awarded_points_by_flag() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("task08-open-student");
        Long studentId = createUser(adminToken, "Task08 Open Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String reviewerEmail = uniqueEmail("task08-open-reviewer");
        Long reviewerId = createUser(adminToken, "Task08 Open Reviewer", reviewerEmail, "ADMIN");
        String reviewerToken = setPasswordAndLogin(reviewerId, reviewerEmail, "Admin123!");

        Long courseId = createCourse(adminToken, "Task08 Open Course");
        enrollStudent(adminToken, courseId, studentId);
        assignReviewer(adminToken, courseId, reviewerId);

        Long lessonShowStatus = createOpenPracticeLesson(adminToken, courseId, "Open show status", true);
        Long submissionShowStatus = submitPracticeAndGetSubmissionId(studentToken, lessonShowStatus, "First open answer");
        reviewSingleOpenQuestion(reviewerToken, submissionShowStatus, 7, "ACCEPTED");

        JsonNode showStatusQuestion = questionByPosition(getLearnerLesson(studentToken, lessonShowStatus), 1);
        assertThat(showStatusQuestion.get("userAnswers").get(0).asText()).isEqualTo("First open answer");
        assertThat(showStatusQuestion.get("status").asText()).isEqualTo("ACCEPTED");
        assertThat(showStatusQuestion.get("awardedPoints").asInt()).isEqualTo(7);

        Long lessonHideStatus = createOpenPracticeLesson(adminToken, courseId, "Open hide status", false);
        Long submissionHideStatus = submitPracticeAndGetSubmissionId(studentToken, lessonHideStatus, "Second open answer");
        reviewSingleOpenQuestion(reviewerToken, submissionHideStatus, 7, "ACCEPTED");

        JsonNode hideStatusQuestion = questionByPosition(getLearnerLesson(studentToken, lessonHideStatus), 1);
        assertThat(hideStatusQuestion.get("userAnswers").get(0).asText()).isEqualTo("Second open answer");
        assertThat(hideStatusQuestion.get("status").isNull()).isTrue();
        assertThat(hideStatusQuestion.get("awardedPoints").isNull()).isTrue();
    }

    @Test
    void options_and_correct_answers_should_be_preserved_with_special_characters() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("task08-converter-student");
        Long studentId = createUser(adminToken, "Task08 Converter Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        Long courseId = createCourse(adminToken, "Task08 Converter Course");
        enrollStudent(adminToken, courseId, studentId);

        Long lessonId = createPracticeTestLesson(adminToken, courseId, "Task08 Converter Lesson", """
                {
                  "title": "%s",
                  "description": "Special chars",
                  "lessonType": "PRACTICE_TEST",
                  "fullPoints": 2,
                  "passingThresholdPercent": 100,
                  "questions": [
                    {
                      "position": 1,
                      "questionType": "MULTIPLE_CHOICE",
                      "questionText": "Special",
                      "options": ["A;;B", "quote \\\"x\\\"", "slash \\\\ path", "normal"],
                      "correctAnswers": ["A;;B", "slash \\\\ path"],
                      "fullPoints": 2,
                      "partialPoints": 1
                    }
                  ]
                }
                """);

        JsonNode adminLessonResponse = objectMapper.readTree(mockMvc.perform(get("/api/v1/admin/courses/{courseId}/lessons/{lessonId}", courseId, lessonId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        JsonNode adminQuestion = questionByPositionFromAdminLesson(adminLessonResponse, 1);
        assertThat(adminQuestion.get("options").get(0).asText()).isEqualTo("A;;B");
        assertThat(adminQuestion.get("options").get(1).asText()).isEqualTo("quote \"x\"");
        assertThat(adminQuestion.get("options").get(2).asText()).isEqualTo("slash \\ path");
        assertThat(adminQuestion.get("correctAnswers").get(0).asText()).isEqualTo("A;;B");
        assertThat(adminQuestion.get("correctAnswers").get(1).asText()).isEqualTo("slash \\ path");

        JsonNode learnerQuestion = questionByPosition(getLearnerLesson(studentToken, lessonId), 1);
        assertThat(learnerQuestion.get("options").get(0).asText()).isEqualTo("A;;B");
        assertThat(learnerQuestion.get("options").get(1).asText()).isEqualTo("quote \"x\"");
        assertThat(learnerQuestion.get("options").get(2).asText()).isEqualTo("slash \\ path");
    }

    private JsonNode getLearnerLesson(String studentToken, Long lessonId) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", lessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    private JsonNode questionByPosition(JsonNode learnerLessonResponse, int position) {
        Iterator<JsonNode> iterator = learnerLessonResponse.get("questions").elements();
        while (iterator.hasNext()) {
            JsonNode question = iterator.next();
            if (question.get("position").asInt() == position) {
                return question;
            }
        }
        throw new IllegalStateException("Question with position=" + position + " not found");
    }

    private JsonNode questionByPositionFromAdminLesson(JsonNode adminLessonResponse, int position) {
        Iterator<JsonNode> iterator = adminLessonResponse.get("questions").elements();
        while (iterator.hasNext()) {
            JsonNode question = iterator.next();
            if (question.get("position").asInt() == position) {
                return question;
            }
        }
        throw new IllegalStateException("Question with position=" + position + " not found");
    }

    private void submitPractice(String studentToken, Long lessonId, String payload) throws Exception {
        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", lessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private Long submitPracticeAndGetSubmissionId(String studentToken, Long lessonId, String answer) throws Exception {
        String response = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", lessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["%s"]
                                  }
                                }
                                """.formatted(answer)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("submissionId").asLong();
    }

    private void reviewSingleOpenQuestion(String reviewerToken,
                                          Long submissionId,
                                          int awardedPoints,
                                          String reviewStatus) throws Exception {
        mockMvc.perform(post("/api/v1/admin/progress/reviews/{submissionId}", submissionId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionReviews": {
                                    "1": {
                                      "submissionStatus": "%s",
                                      "awardedPoints": %d,
                                      "reviewComment": "ok"
                                    }
                                  }
                                }
                                """.formatted(reviewStatus, awardedPoints)))
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
                                  "description": "Task08 course",
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

    private void assignReviewer(String adminToken, Long courseId, Long reviewerId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/reviewers", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(reviewerId)))
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

    private Long createOpenPracticeLesson(String adminToken,
                                          Long courseId,
                                          String title,
                                          boolean showQuestionStatus) throws Exception {
        String createLessonResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Task08 open lesson",
                                  "lessonType": "PRACTICE_OPEN_ANSWER",
                                  "showQuestionStatus": %s,
                                  "showCorrectAnswers": true,
                                  "fullPoints": 10,
                                  "passingThresholdPercent": 70,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "OPEN_ANSWER",
                                      "questionText": "Explain JVM",
                                      "trainerHint": "Mention details",
                                      "fullPoints": 10
                                    }
                                  ]
                                }
                                """.formatted(title, showQuestionStatus)))
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
