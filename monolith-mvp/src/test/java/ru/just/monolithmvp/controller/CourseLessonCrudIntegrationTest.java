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
import ru.just.monolithmvp.model.LessonSubmission;
import ru.just.monolithmvp.model.PasswordSetupToken;
import ru.just.monolithmvp.model.SubmissionStatus;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.repository.PasswordSetupTokenRepository;

import java.util.UUID;

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

    @Autowired
    private LessonSubmissionRepository lessonSubmissionRepository;

    @Test
    void theory_lesson_completion_should_update_progress_and_stats() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("theory-student");
        Long studentId = createUser(adminToken, "Theory Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Theory Completion Course",
                                  "description": "FR-012 checks",
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
                                  "title": "Theory FR-012",
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

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", theoryLessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/assign", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", theoryLessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        String completeTheoryResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/complete-theory", theoryLessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode completeTheoryResult = objectMapper.readTree(completeTheoryResponse);
        assertThat(completeTheoryResult.get("status").asText()).isEqualTo("COMPLETE");
        assertThat(completeTheoryResult.get("passed").asBoolean()).isTrue();

        String myStatsResponse = mockMvc.perform(get("/api/v1/student/my/stats")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode myStats = objectMapper.readTree(myStatsResponse);
        assertThat(myStats.size()).isEqualTo(1);
        JsonNode myCourseStat = myStats.get(0);
        assertThat(myCourseStat.get("courseId").asLong()).isEqualTo(courseId);
        assertThat(myCourseStat.get("earnedPoints").asInt()).isEqualTo(5);
        assertThat(myCourseStat.get("maxPoints").asInt()).isEqualTo(5);
        assertThat(myCourseStat.get("efficiencyPercent").asDouble()).isEqualTo(100.0);
        assertThat(myCourseStat.get("progressPercent").asDouble()).isEqualTo(100.0);
        assertThat(myCourseStat.get("completedLessons").asInt()).isEqualTo(1);
        assertThat(myCourseStat.get("totalLessons").asInt()).isEqualTo(1);

        String adminUserStatsResponse = mockMvc.perform(get("/api/v1/admin/users/{userId}/stats", studentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode adminUserStats = objectMapper.readTree(adminUserStatsResponse);
        assertThat(adminUserStats.size()).isEqualTo(1);
        JsonNode adminUserCourseStat = adminUserStats.get(0);
        assertThat(adminUserCourseStat.get("courseId").asLong()).isEqualTo(courseId);
        assertThat(adminUserCourseStat.get("earnedPoints").asInt()).isEqualTo(5);
        assertThat(adminUserCourseStat.get("maxPoints").asInt()).isEqualTo(5);
        assertThat(adminUserCourseStat.get("efficiencyPercent").asDouble()).isEqualTo(100.0);
        assertThat(adminUserCourseStat.get("progressPercent").asDouble()).isEqualTo(100.0);
        assertThat(adminUserCourseStat.get("completedLessons").asInt()).isEqualTo(1);
        assertThat(adminUserCourseStat.get("totalLessons").asInt()).isEqualTo(1);

        String courseStatsResponse = mockMvc.perform(get("/api/v1/admin/progress/courses/{courseId}/stats", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode courseStats = objectMapper.readTree(courseStatsResponse);
        assertThat(courseStats.size()).isEqualTo(1);
        JsonNode studentCourseStat = courseStats.get(0);
        assertThat(studentCourseStat.get("studentId").asLong()).isEqualTo(studentId);
        assertThat(studentCourseStat.get("fullName").asText()).isEqualTo("Theory Student");
        assertThat(studentCourseStat.get("earnedPoints").asInt()).isEqualTo(5);
        assertThat(studentCourseStat.get("maxPoints").asInt()).isEqualTo(5);
        assertThat(studentCourseStat.get("efficiencyPercent").asDouble()).isEqualTo(100.0);
        assertThat(studentCourseStat.get("progressPercent").asDouble()).isEqualTo(100.0);
        assertThat(studentCourseStat.get("completedLessons").asInt()).isEqualTo(1);
        assertThat(studentCourseStat.get("totalLessons").asInt()).isEqualTo(1);
    }

    @Test
    void course_stats_should_return_progress_percent_and_match_user_stats() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Course Stats FR-016",
                                  "description": "FR-016 checks",
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
                                  "title": "Stats Theory",
                                  "description": "One lesson",
                                  "contentType": "HTML_TEXT",
                                  "content": "Theory",
                                  "fullPoints": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long theoryLessonId = objectMapper.readTree(theoryLessonResponse).get("id").asLong();

        String studentDoneEmail = uniqueEmail("stats-done");
        Long studentDoneId = createUser(adminToken, "Stats Done", studentDoneEmail, "STUDENT");
        String studentDoneToken = setPasswordAndLogin(studentDoneId, studentDoneEmail, "Stud123!");

        String studentNewEmail = uniqueEmail("stats-new");
        Long studentNewId = createUser(adminToken, "Stats New", studentNewEmail, "STUDENT");
        setPasswordAndLogin(studentNewId, studentNewEmail, "Stud123!");

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/assign", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d, %d]
                                }
                                """.formatted(studentDoneId, studentNewId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/complete-theory", theoryLessonId)
                        .header("Authorization", "Bearer " + studentDoneToken))
                .andExpect(status().isOk());

        String courseStatsResponse = mockMvc.perform(get("/api/v1/admin/progress/courses/{courseId}/stats", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode courseStats = objectMapper.readTree(courseStatsResponse);
        assertThat(courseStats.size()).isEqualTo(2);

        JsonNode doneCourseStat = null;
        JsonNode newCourseStat = null;
        for (JsonNode stat : courseStats) {
            if (stat.get("studentId").asLong() == studentDoneId) {
                doneCourseStat = stat;
            }
            if (stat.get("studentId").asLong() == studentNewId) {
                newCourseStat = stat;
            }
        }

        assertThat(doneCourseStat).isNotNull();
        assertThat(doneCourseStat.get("fullName").asText()).isEqualTo("Stats Done");
        assertThat(doneCourseStat.get("earnedPoints").asInt()).isEqualTo(10);
        assertThat(doneCourseStat.get("maxPoints").asInt()).isEqualTo(10);
        assertThat(doneCourseStat.get("efficiencyPercent").asDouble()).isEqualTo(100.0);
        assertThat(doneCourseStat.get("progressPercent").asDouble()).isEqualTo(100.0);
        assertThat(doneCourseStat.get("completedLessons").asInt()).isEqualTo(1);
        assertThat(doneCourseStat.get("totalLessons").asInt()).isEqualTo(1);

        assertThat(newCourseStat).isNotNull();
        assertThat(newCourseStat.get("fullName").asText()).isEqualTo("Stats New");
        assertThat(newCourseStat.get("earnedPoints").asInt()).isEqualTo(0);
        assertThat(newCourseStat.get("maxPoints").asInt()).isEqualTo(10);
        assertThat(newCourseStat.get("efficiencyPercent").asDouble()).isEqualTo(0.0);
        assertThat(newCourseStat.get("progressPercent").asDouble()).isEqualTo(0.0);
        assertThat(newCourseStat.get("completedLessons").asInt()).isEqualTo(0);
        assertThat(newCourseStat.get("totalLessons").asInt()).isEqualTo(1);

        String doneUserStatsResponse = mockMvc.perform(get("/api/v1/admin/users/{userId}/stats", studentDoneId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode doneUserStats = objectMapper.readTree(doneUserStatsResponse);
        JsonNode doneUserCourseStat = doneUserStats.get(0);
        assertThat(doneUserCourseStat.get("courseId").asLong()).isEqualTo(courseId);
        assertThat(doneUserCourseStat.get("earnedPoints").asInt()).isEqualTo(doneCourseStat.get("earnedPoints").asInt());
        assertThat(doneUserCourseStat.get("maxPoints").asInt()).isEqualTo(doneCourseStat.get("maxPoints").asInt());
        assertThat(doneUserCourseStat.get("efficiencyPercent").asDouble()).isEqualTo(doneCourseStat.get("efficiencyPercent").asDouble());
        assertThat(doneUserCourseStat.get("progressPercent").asDouble()).isEqualTo(doneCourseStat.get("progressPercent").asDouble());

        String newUserStatsResponse = mockMvc.perform(get("/api/v1/admin/users/{userId}/stats", studentNewId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode newUserStats = objectMapper.readTree(newUserStatsResponse);
        JsonNode newUserCourseStat = newUserStats.get(0);
        assertThat(newUserCourseStat.get("courseId").asLong()).isEqualTo(courseId);
        assertThat(newUserCourseStat.get("earnedPoints").asInt()).isEqualTo(newCourseStat.get("earnedPoints").asInt());
        assertThat(newUserCourseStat.get("maxPoints").asInt()).isEqualTo(newCourseStat.get("maxPoints").asInt());
        assertThat(newUserCourseStat.get("efficiencyPercent").asDouble()).isEqualTo(newCourseStat.get("efficiencyPercent").asDouble());
        assertThat(newUserCourseStat.get("progressPercent").asDouble()).isEqualTo(newCourseStat.get("progressPercent").asDouble());
    }

    @Test
    void enrollment_two_lists_flow_should_work() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String enrolledCandidateEmail = uniqueEmail("enrolled-candidate");
        Long enrolledCandidateId = createUser(adminToken, "Enrolled Candidate", enrolledCandidateEmail, "STUDENT");
        String enrolledCandidateToken = setPasswordAndLogin(enrolledCandidateId, enrolledCandidateEmail, "Stud123!");

        String notEnrolledCandidateEmail = uniqueEmail("not-enrolled-candidate");
        Long notEnrolledCandidateId = createUser(adminToken, "Not Enrolled Candidate", notEnrolledCandidateEmail, "STUDENT");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Enrollment Two Lists Course",
                                  "description": "FR-010 checks",
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

        String initialListsResponse = mockMvc.perform(get("/api/v1/admin/courses/{courseId}/enrollments/lists", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode initialLists = objectMapper.readTree(initialListsResponse);
        assertThat(initialLists.get("enrolled").isEmpty()).isTrue();
        assertThat(initialLists.get("notEnrolled").toString()).contains("\"id\":" + enrolledCandidateId);
        assertThat(initialLists.get("notEnrolled").toString()).contains("\"id\":" + notEnrolledCandidateId);

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d]
                                }
                                """.formatted(enrolledCandidateId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + enrolledCandidateToken))
                .andExpect(status().isOk());

        String afterEnrollListsResponse = mockMvc.perform(get("/api/v1/admin/courses/{courseId}/enrollments/lists", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode afterEnrollLists = objectMapper.readTree(afterEnrollListsResponse);
        assertThat(afterEnrollLists.get("enrolled").toString()).contains("\"id\":" + enrolledCandidateId);
        assertThat(afterEnrollLists.get("notEnrolled").toString()).doesNotContain("\"id\":" + enrolledCandidateId);

        mockMvc.perform(delete("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d]
                                }
                                """.formatted(enrolledCandidateId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + enrolledCandidateToken))
                .andExpect(status().isBadRequest());

        String afterUnenrollListsResponse = mockMvc.perform(get("/api/v1/admin/courses/{courseId}/enrollments/lists", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode afterUnenrollLists = objectMapper.readTree(afterUnenrollListsResponse);
        assertThat(afterUnenrollLists.get("enrolled").toString()).doesNotContain("\"id\":" + enrolledCandidateId);
        assertThat(afterUnenrollLists.get("notEnrolled").toString()).contains("\"id\":" + enrolledCandidateId);
    }

    @Test
    void practice_lesson_should_support_all_question_types_and_partial_scoring() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("practice-types-student");
        Long studentId = createUser(adminToken, "Practice Types Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Practice Types Course",
                                  "description": "FR-008/FR-009 checks",
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

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/assign", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        String practiceLessonResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Practice All Types",
                                  "description": "All question types",
                                  "lessonType": "PRACTICE_TEST",
                                  "passingThresholdPercent": 60,
                                  "fullPoints": 1,
                                  "partialPoints": 0,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "2+2",
                                      "options": ["3", "4"],
                                      "correctAnswers": ["4"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 2,
                                      "questionType": "MULTIPLE_CHOICE",
                                      "questionText": "Prime numbers",
                                      "options": ["2", "3", "4"],
                                      "correctAnswers": ["2", "3"],
                                      "fullPoints": 2,
                                      "partialPoints": 1
                                    },
                                    {
                                      "position": 3,
                                      "questionType": "MATCHING",
                                      "questionText": "Match Java",
                                      "options": ["JVM=runtime", "JDK=tools"],
                                      "correctAnswers": ["JVM=runtime", "JDK=tools"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 4,
                                      "questionType": "ORDERING",
                                      "questionText": "Order numbers",
                                      "options": ["1", "2", "3"],
                                      "correctAnswers": ["1", "2", "3"],
                                      "fullPoints": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long practiceLessonId = objectMapper.readTree(practiceLessonResponse).get("id").asLong();

        String submitPracticeResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", practiceLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["4"],
                                    "2": ["2"],
                                    "3": ["JVM=runtime", "JDK=tools"],
                                    "4": ["3", "2", "1"]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode submitPracticeResult = objectMapper.readTree(submitPracticeResponse);
        assertThat(submitPracticeResult.get("status").asText()).isEqualTo("COMPLETE");
        assertThat(submitPracticeResult.get("passed").asBoolean()).isTrue();

        LessonSubmission submission = lessonSubmissionRepository.findAll().stream()
                .filter(s -> s.getLesson().getId().equals(practiceLessonId) && s.getStudent().getId().equals(studentId))
                .reduce((a, b) -> b)
                .orElseThrow();

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.COMPLETE);
        assertThat(submission.getPassed()).isTrue();
        assertThat(submission.getPointsAwarded()).isEqualTo(3);

        String myStatsResponse = mockMvc.perform(get("/api/v1/student/my/stats")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode myStats = objectMapper.readTree(myStatsResponse);
        assertThat(myStats.size()).isEqualTo(1);
        JsonNode courseStat = myStats.get(0);
        assertThat(courseStat.get("courseId").asLong()).isEqualTo(courseId);
        assertThat(courseStat.get("earnedPoints").asInt()).isEqualTo(3);
        assertThat(courseStat.get("maxPoints").asInt()).isEqualTo(1);
        assertThat(courseStat.get("efficiencyPercent").asDouble()).isEqualTo(300.0);
        assertThat(courseStat.get("progressPercent").asDouble()).isEqualTo(100.0);
        assertThat(courseStat.get("completedLessons").asInt()).isEqualTo(1);
        assertThat(courseStat.get("totalLessons").asInt()).isEqualTo(1);
    }

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

        String createAdminLearnerResponse = mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Admin Learner",
                                  "email": "admin-learner@example.com",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long adminLearnerId = objectMapper.readTree(createAdminLearnerResponse).get("id").asLong();

        PasswordSetupToken adminLearnerInviteToken = passwordSetupTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(adminLearnerId))
                .reduce((a, b) -> b)
                .orElseThrow();

        mockMvc.perform(post("/api/v1/auth/set-password?token=" + adminLearnerInviteToken.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "Admin123!"
                                }
                                """))
                .andExpect(status().isOk());

        String adminLearnerToken = login("admin-learner@example.com", "Admin123!");

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/assign", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d, %d]
                                }
                                """.formatted(studentId, adminLearnerId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", theoryLessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", practiceLessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/complete-theory", theoryLessonId)
                        .header("Authorization", "Bearer " + adminLearnerToken))
                .andExpect(status().isOk());

        String openPracticeResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Practice Open",
                                  "description": "Open answer",
                                  "lessonType": "PRACTICE_OPEN_ANSWER",
                                  "fullPoints": 7,
                                  "partialPoints": 2,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "OPEN_ANSWER",
                                      "questionText": "Explain JVM",
                                      "trainerHint": "Provide details",
                                      "fullPoints": 7,
                                      "partialPoints": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long openPracticeLessonId = objectMapper.readTree(openPracticeResponse).get("id").asLong();

        String openSubmissionResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", openPracticeLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "openAnswer": "My open answer"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long openSubmissionId = objectMapper.readTree(openSubmissionResponse).get("submissionId").asLong();

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/reviewers", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d]
                                }
                                """.formatted(adminLearnerId)))
                .andExpect(status().isOk());

        String reReviewResponse = mockMvc.perform(post("/api/v1/admin/progress/reviews/{submissionId}", openSubmissionId)
                        .header("Authorization", "Bearer " + adminLearnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passed": true,
                                  "partialPoints": true,
                                  "toNextReview": true,
                                  "comment": "Need second pass"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode reReview = objectMapper.readTree(reReviewResponse);
        assertThat(reReview.get("status").asText()).isEqualTo("REWORK");
        assertThat(reReview.get("passed").asBoolean()).isFalse();

        String learnerCourseResponse = mockMvc.perform(get("/api/v1/student/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode learnerCourse = objectMapper.readTree(learnerCourseResponse);
        assertThat(learnerCourse.get("lessons").size()).isEqualTo(3);

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
                                    "%d": 1,
                                    "%d": 3
                                  }
                                }
                                """.formatted(theoryLessonId, practiceLessonId, openPracticeLessonId)))
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
        assertThat(adminCourse.get("lessons").get(2).get("id").asLong()).isEqualTo(openPracticeLessonId);
        assertThat(adminCourse.get("lessons").get(2).get("position").asInt()).isEqualTo(3);

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

        mockMvc.perform(delete("/api/v1/admin/courses/{courseId}/lessons/{lessonId}", courseId, openPracticeLessonId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        String afterDeleteLessonCourse = mockMvc.perform(get("/api/v1/admin/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode afterDeleteLesson = objectMapper.readTree(afterDeleteLessonCourse);
        assertThat(afterDeleteLesson.get("lessons").size()).isEqualTo(2);
        assertThat(afterDeleteLesson.get("lessons").get(0).get("id").asLong()).isEqualTo(practiceLessonId);
        assertThat(afterDeleteLesson.get("lessons").get(0).get("position").asInt()).isEqualTo(1);
        assertThat(afterDeleteLesson.get("lessons").get(1).get("id").asLong()).isEqualTo(theoryLessonId);
        assertThat(afterDeleteLesson.get("lessons").get(1).get("position").asInt()).isEqualTo(2);

        mockMvc.perform(delete("/api/v1/admin/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void validation_and_negative_cases_for_courses_and_lessons_should_work() throws Exception {
        String adminToken = login("admin@local", "admin123");

        mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid Course",
                                  "description": "Should fail",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 120,
                                  "deadlineDays": 0
                                }
                                """))
                .andExpect(status().isBadRequest());

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Validation Course",
                                  "description": "For negative checks",
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

        String theoryResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/theory", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Theory",
                                  "description": "Theory",
                                  "contentType": "HTML_TEXT",
                                  "content": "text",
                                  "fullPoints": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long theoryLessonId = objectMapper.readTree(theoryResponse).get("id").asLong();

        String openPracticeResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Open Practice",
                                  "description": "Open answer",
                                  "lessonType": "PRACTICE_OPEN_ANSWER",
                                  "fullPoints": 5,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "OPEN_ANSWER",
                                      "questionText": "Explain",
                                      "trainerHint": "Hint",
                                      "fullPoints": 5
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long openPracticeLessonId = objectMapper.readTree(openPracticeResponse).get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Validation Course Updated",
                                  "description": "Updated",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 80,
                                  "deadlineDays": 40,
                                  "lessonIdToPosition": {
                                    "%d": 1
                                  }
                                }
                                """.formatted(theoryLessonId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/admin/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Validation Course Updated",
                                  "description": "Updated",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 80,
                                  "deadlineDays": 40,
                                  "lessonIdToPosition": {
                                    "%d": 2,
                                    "%d": 2
                                  }
                                }
                                """.formatted(theoryLessonId, openPracticeLessonId)))
                .andExpect(status().isBadRequest());

        Long studentId = createUser(adminToken, "Reviewer Student", uniqueEmail("reviewer-student"), "STUDENT");
        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/reviewers", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enrollment_submission_and_review_corner_cases_should_work() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("corner-student");
        Long studentId = createUser(adminToken, "Corner Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String otherAdminEmail = uniqueEmail("corner-admin");
        Long otherAdminId = createUser(adminToken, "Corner Admin", otherAdminEmail, "ADMIN");
        String otherAdminToken = setPasswordAndLogin(otherAdminId, otherAdminEmail, "Admin123!");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Corner Course",
                                  "description": "Corner cases",
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

        mockMvc.perform(get("/api/v1/student/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/assign", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/assign", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        String myCoursesResponse = mockMvc.perform(get("/api/v1/student/my/courses")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(myCoursesResponse).size()).isEqualTo(1);

        String testPracticeResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test Practice",
                                  "description": "Choose",
                                  "lessonType": "PRACTICE_TEST",
                                  "fullPoints": 2,
                                  "partialPoints": 1,
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
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long testPracticeLessonId = objectMapper.readTree(testPracticeResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", testPracticeLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "openAnswer": "ignored"
                                }
                                """))
                .andExpect(status().isBadRequest());

        String openPracticeResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Open Practice",
                                  "description": "Open",
                                  "lessonType": "PRACTICE_OPEN_ANSWER",
                                  "fullPoints": 3,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "OPEN_ANSWER",
                                      "questionText": "Explain JVM",
                                      "trainerHint": "Hint",
                                      "fullPoints": 3
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long openPracticeLessonId = objectMapper.readTree(openPracticeResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", openPracticeLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "selectedAnswers": ["x"]
                                }
                                """))
                .andExpect(status().isBadRequest());

        String openSubmissionResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", openPracticeLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "openAnswer": "My detailed answer"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long openSubmissionId = objectMapper.readTree(openSubmissionResponse).get("submissionId").asLong();

        mockMvc.perform(post("/api/v1/admin/progress/reviews/{submissionId}", openSubmissionId)
                        .header("Authorization", "Bearer " + otherAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passed": true,
                                  "partialPoints": false,
                                  "toNextReview": false,
                                  "comment": "Try"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/reviewers", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d]
                                }
                                """.formatted(otherAdminId)))
                .andExpect(status().isOk());

        String reviewerCoursesResponse = mockMvc.perform(get("/api/v1/admin/progress/reviews/courses")
                        .header("Authorization", "Bearer " + otherAdminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode reviewerCourses = objectMapper.readTree(reviewerCoursesResponse);
        assertThat(reviewerCourses.size()).isEqualTo(1);
        assertThat(reviewerCourses.get(0).get("id").asLong()).isEqualTo(courseId);

        String pendingReviewsResponse = mockMvc.perform(get("/api/v1/admin/progress/reviews/pending")
                        .header("Authorization", "Bearer " + otherAdminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode pendingReviews = objectMapper.readTree(pendingReviewsResponse);
        assertThat(pendingReviews.size()).isEqualTo(1);
        assertThat(pendingReviews.get(0).get("submissionId").asLong()).isEqualTo(openSubmissionId);

        mockMvc.perform(post("/api/v1/admin/progress/reviews/{submissionId}", openSubmissionId)
                        .header("Authorization", "Bearer " + otherAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passed": true,
                                  "partialPoints": false,
                                  "toNextReview": false,
                                  "comment": "Approved"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/progress/reviews/{submissionId}", openSubmissionId)
                        .header("Authorization", "Bearer " + otherAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passed": true,
                                  "partialPoints": false,
                                  "toNextReview": false,
                                  "comment": "Second review should fail"
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

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
