package ru.just.monolithmvp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.just.monolithmvp.model.PasswordSetupToken;
import ru.just.monolithmvp.repository.PasswordSetupTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:course-lesson-crud-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.profiles.active=h2"
})
class CourseLessonCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordSetupTokenRepository passwordSetupTokenRepository;

    @Test
    void admin_and_student_course_lesson_crud_flow_should_work() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String createStudentResponse = mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Course Student",
                                  "email": "course-student@example.com",
                                  "role": "STUDENT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long studentId = objectMapper.readTree(createStudentResponse).get("id").asLong();

        PasswordSetupToken inviteToken = passwordSetupTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(studentId))
                .reduce((a, b) -> b)
                .orElseThrow();

        mockMvc.perform(post("/api/v1/auth/set-password?token=" + inviteToken.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "Stud123!"
                                }
                                """))
                .andExpect(status().isOk());

        String studentToken = login("course-student@example.com", "Stud123!");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Java Core",
                                  "description": "Base course",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 70,
                                  "deadlineDays": 30
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long courseId = objectMapper.readTree(createCourseResponse).get("id").asLong();

        String theoryLessonResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/theory", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Theory 1",
                                  "description": "Theory description",
                                  "contentType": "HTML_TEXT",
                                  "content": "Theory content",
                                  "fullPoints": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long theoryLessonId = objectMapper.readTree(theoryLessonResponse).get("id").asLong();

        // Негативный кейс: Указаны только некоторые позиции
        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Practice 1",
                                  "description": "Practice description",
                                  "lessonType": "PRACTICE_TEST",
                                  "fullPoints": 10,
                                  "partialPoints": 3,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "2 + 2 = ?",
                                      "options": ["3", "4"],
                                      "correctAnswers": ["4"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "questionType": "MULTIPLE_CHOICE",
                                      "questionText": "Prime numbers",
                                      "options": ["2", "4", "5"],
                                      "correctAnswers": ["2", "5"],
                                      "fullPoints": 2,
                                      "partialPoints": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Негативный кейс: Указаны позиции не по порядку
        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Practice 1",
                                  "description": "Practice description",
                                  "lessonType": "PRACTICE_TEST",
                                  "fullPoints": 10,
                                  "partialPoints": 3,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "2 + 2 = ?",
                                      "options": ["3", "4"],
                                      "correctAnswers": ["4"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 3,
                                      "questionType": "MULTIPLE_CHOICE",
                                      "questionText": "Prime numbers",
                                      "options": ["2", "4", "5"],
                                      "correctAnswers": ["2", "5"],
                                      "fullPoints": 2,
                                      "partialPoints": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String practiceLessonResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Practice 1",
                                  "description": "Practice description",
                                  "lessonType": "PRACTICE_TEST",
                                  "fullPoints": 10,
                                  "partialPoints": 3,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "2 + 2 = ?",
                                      "options": ["3", "4"],
                                      "correctAnswers": ["4"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 2,
                                      "questionType": "MULTIPLE_CHOICE",
                                      "questionText": "Prime numbers",
                                      "options": ["2", "4", "5"],
                                      "correctAnswers": ["2", "5"],
                                      "fullPoints": 2,
                                      "partialPoints": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long practiceLessonId = objectMapper.readTree(practiceLessonResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/assign", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        String learnerCourseResponse = mockMvc.perform(get("/api/v1/student/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode learnerCourse = objectMapper.readTree(learnerCourseResponse);
        assertThat(learnerCourse.get("lessons").size()).isEqualTo(2);

        mockMvc.perform(put("/api/v1/admin/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Java Core Updated",
                                  "description": "Base course updated",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 75,
                                  "deadlineDays": 45,
                                  "lessonIdToPosition": {
                                    "%d": 2,
                                    "%d": 1
                                  }
                                }
                                """.formatted(theoryLessonId, practiceLessonId)))
                .andExpect(status().isOk());

        String adminCourseResponse = mockMvc.perform(get("/api/v1/admin/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode adminCourse = objectMapper.readTree(adminCourseResponse);
        assertThat(adminCourse.get("lessons").get(0).get("id").asLong()).isEqualTo(practiceLessonId);
        assertThat(adminCourse.get("lessons").get(0).get("position").asInt()).isEqualTo(1);
        assertThat(adminCourse.get("lessons").get(1).get("id").asLong()).isEqualTo(theoryLessonId);
        assertThat(adminCourse.get("lessons").get(1).get("position").asInt()).isEqualTo(2);

        mockMvc.perform(put("/api/v1/admin/courses/{courseId}/lessons/{lessonId}/practice", courseId, practiceLessonId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Practice 1 Updated",
                                  "description": "Practice description updated",
                                  "lessonType": "PRACTICE_TEST",
                                  "fullPoints": 12,
                                  "partialPoints": 4,
                                  "questions": [
                                    {
                                      "position": 2,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "2 + 2 = ?",
                                      "options": ["3", "4"],
                                      "correctAnswers": ["4"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 1,
                                      "questionType": "MULTIPLE_CHOICE",
                                      "questionText": "Prime numbers updated",
                                      "options": ["2", "4", "5"],
                                      "correctAnswers": ["2", "5"],
                                      "fullPoints": 2,
                                      "partialPoints": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        String updatedPracticeResponse = mockMvc.perform(get("/api/v1/admin/courses/{courseId}/lessons/{lessonId}", courseId, practiceLessonId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode updatedPractice = objectMapper.readTree(updatedPracticeResponse);
        assertThat(updatedPractice.get("title").asText()).isEqualTo("Practice 1 Updated");
        assertThat(updatedPractice.get("questions").get(0).get("index").asInt()).isEqualTo(1);
        assertThat(updatedPractice.get("questions").get(0).get("questionText").asText())
                .isEqualTo("Prime numbers updated");

        mockMvc.perform(delete("/api/v1/admin/courses/{courseId}/lessons/{lessonId}", courseId, theoryLessonId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        String afterDeleteLessonCourse = mockMvc.perform(get("/api/v1/admin/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode afterDeleteLesson = objectMapper.readTree(afterDeleteLessonCourse);
        assertThat(afterDeleteLesson.get("lessons").size()).isEqualTo(1);
        assertThat(afterDeleteLesson.get("lessons").get(0).get("id").asLong()).isEqualTo(practiceLessonId);
        assertThat(afterDeleteLesson.get("lessons").get(0).get("position").asInt()).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/admin/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
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
