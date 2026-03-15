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
import ru.just.monolithmvp.model.LessonSubmission;
import ru.just.monolithmvp.model.PasswordSetupToken;
import ru.just.monolithmvp.model.SubmissionStatus;
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
        "spring.datasource.url=jdbc:h2:mem:task06-review-flow-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.profiles.active=h2,mail-noop"
})
class Task06ReviewFlowIntegrationTest {

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
    void open_review_flow_should_keep_rework_in_pending_and_finalize_with_complete() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("task06-student");
        Long studentId = createUser(adminToken, "Task06 Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String reviewerEmail = uniqueEmail("task06-reviewer");
        Long reviewerId = createUser(adminToken, "Task06 Reviewer", reviewerEmail, "ADMIN");
        String reviewerToken = setPasswordAndLogin(reviewerId, reviewerEmail, "Admin123!");

        String outsiderEmail = uniqueEmail("task06-outsider");
        Long outsiderId = createUser(adminToken, "Task06 Outsider", outsiderEmail, "ADMIN");
        String outsiderToken = setPasswordAndLogin(outsiderId, outsiderEmail, "Admin123!");

        Long courseId = createCourse(adminToken, "Task06 Review Course");
        enrollStudent(adminToken, courseId, studentId);
        assignReviewer(adminToken, courseId, reviewerId);

        Long openLessonId = createOpenPracticeLesson(adminToken, courseId, "Task06 Open Lesson");

        String submitResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", openLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["JVM answer"],
                                    "2": ["JIT answer"]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long submissionId = objectMapper.readTree(submitResponse).get("submissionId").asLong();

        mockMvc.perform(post("/api/v1/admin/progress/reviews/{submissionId}", submissionId)
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionReviews": {
                                    "1": {"submissionStatus": "ACCEPTED", "awardedPoints": 7, "reviewComment": "ok"},
                                    "2": {"submissionStatus": "ACCEPTED", "awardedPoints": 7, "reviewComment": "ok"}
                                  }
                                }
                                """))
                .andExpect(status().isForbidden());

        String reviewerPendingResponse = mockMvc.perform(get("/api/v1/admin/progress/reviews/pending")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode reviewerPending = objectMapper.readTree(reviewerPendingResponse);
        assertThat(reviewerPending).hasSize(1);
        assertThat(reviewerPending.get(0).get("submissionId").asLong()).isEqualTo(submissionId);

        String pendingQuestionsResponse = mockMvc.perform(get("/api/v1/admin/progress/reviews/pending/{submissionId}", submissionId)
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode pendingQuestions = objectMapper.readTree(pendingQuestionsResponse);
        assertThat(pendingQuestions).hasSize(2);
        assertThat(pendingQuestions.get(0).get("submissionStatus").asText()).isEqualTo("PENDING_REVIEW");
        assertThat(pendingQuestions.get(1).get("submissionStatus").asText()).isEqualTo("PENDING_REVIEW");

        String reworkReviewResponse = mockMvc.perform(post("/api/v1/admin/progress/reviews/{submissionId}", submissionId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionReviews": {
                                    "1": {"submissionStatus": "ACCEPTED", "awardedPoints": 7, "reviewComment": "Good"},
                                    "2": {"submissionStatus": "REWORK", "awardedPoints": 6, "reviewComment": "Need rework"}
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode reworkReview = objectMapper.readTree(reworkReviewResponse);
        assertThat(reworkReview.get("status").asText()).isEqualTo("REWORK");
        assertThat(reworkReview.get("completed").asBoolean()).isFalse();

        LessonSubmission afterRework = lessonSubmissionRepository.findById(submissionId).orElseThrow();
        assertThat(afterRework.getStatus()).isEqualTo(SubmissionStatus.REWORK);
        assertThat(afterRework.getCompleted()).isFalse();
        assertThat(afterRework.getPointsAwarded()).isZero();

        String pendingAfterReworkResponse = mockMvc.perform(get("/api/v1/admin/progress/reviews/pending")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode pendingAfterRework = objectMapper.readTree(pendingAfterReworkResponse);
        assertThat(pendingAfterRework).isEmpty();

        String pendingQuestionsAfterReworkResponse = mockMvc.perform(get("/api/v1/admin/progress/reviews/pending/{submissionId}", submissionId)
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode pendingQuestionsAfterRework = objectMapper.readTree(pendingQuestionsAfterReworkResponse);
        JsonNode secondQuestion = pendingQuestionsAfterRework.get(1);
        assertThat(secondQuestion.get("submissionStatus").asText()).isEqualTo("REWORK");
        assertThat(secondQuestion.get("awardedPoints").asInt()).isZero();

        String finalizeReviewResponse = mockMvc.perform(post("/api/v1/admin/progress/reviews/{submissionId}", submissionId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionReviews": {
                                    "1": {"submissionStatus": "ACCEPTED", "awardedPoints": 9, "reviewComment": "Accepted after rework"},
                                    "2": {"submissionStatus": "ACCEPTED", "awardedPoints": 8, "reviewComment": "Accepted"}
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode finalizeReview = objectMapper.readTree(finalizeReviewResponse);
        assertThat(finalizeReview.get("status").asText()).isEqualTo("COMPLETE");
        assertThat(finalizeReview.get("completed").asBoolean()).isTrue();

        LessonSubmission finalizedSubmission = lessonSubmissionRepository.findById(submissionId).orElseThrow();
        assertThat(finalizedSubmission.getStatus()).isEqualTo(SubmissionStatus.COMPLETE);
        assertThat(finalizedSubmission.getCompleted()).isTrue();
        assertThat(finalizedSubmission.getPointsAwarded()).isEqualTo(10);

        String pendingAfterFinalizeResponse = mockMvc.perform(get("/api/v1/admin/progress/reviews/pending")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode pendingAfterFinalize = objectMapper.readTree(pendingAfterFinalizeResponse);
        assertThat(pendingAfterFinalize).isEmpty();

        mockMvc.perform(get("/api/v1/admin/progress/reviews/pending/{submissionId}", submissionId)
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/admin/progress/reviews/{submissionId}", submissionId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionReviews": {
                                    "1": {"submissionStatus": "ACCEPTED", "awardedPoints": 9, "reviewComment": "Late"},
                                    "2": {"submissionStatus": "ACCEPTED", "awardedPoints": 10, "reviewComment": "Late"}
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());
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
                                  "description": "Task06 course",
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

    private Long createOpenPracticeLesson(String adminToken, Long courseId, String title) throws Exception {
        String createLessonResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Task06 open lesson",
                                  "lessonType": "PRACTICE_OPEN_ANSWER",
                                  "fullPoints": 10,
                                  "passingThresholdPercent": 70,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "OPEN_ANSWER",
                                      "questionText": "Explain JVM",
                                      "trainerHint": "Mention class loading",
                                      "fullPoints": 10
                                    },
                                    {
                                      "position": 2,
                                      "questionType": "OPEN_ANSWER",
                                      "questionText": "Explain JIT",
                                      "trainerHint": "Mention compilation",
                                      "fullPoints": 10
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
}
