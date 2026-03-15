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
import ru.just.monolithmvp.model.CourseProgressStatus;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.CourseProgressRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.repository.PasswordSetupTokenRepository;

import java.time.LocalDateTime;
import java.util.*;

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
        "spring.profiles.active=h2,mail-noop"
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

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void alignH2QuestionProgressColumnForTargetConverterFlow() {
        jdbcTemplate.execute("alter table lesson_submissions alter column question_progress_json varchar");
    }

    @Test
    void stop_lesson_should_block_next_lessons_even_when_lessons_free_order_enabled() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("stop-lesson-student");
        Long studentId = createUser(adminToken, "Stop Lesson Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Stop Lesson Course",
                                  "description": "FR-114 checks",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 70,
                                  "deadlineDays": 30,
                                  "lessonsFreeOrder": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long courseId = objectMapper.readTree(createCourseResponse).get("id").asLong();

        String blockingTheoryResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/theory", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Blocking Theory",
                                  "description": "Must be completed first",
                                  "lessonType": "THEORY_TEXT",
                                  "content": "Blocking content",
                                  "fullPoints": 5,
                                  "stopLesson": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long blockingTheoryId = objectMapper.readTree(blockingTheoryResponse).get("id").asLong();

        String nextTheoryResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/theory", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Next Theory",
                                  "description": "Should be blocked until first completed",
                                  "lessonType": "THEORY_TEXT",
                                  "content": "Next content",
                                  "fullPoints": 4
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long nextTheoryId = objectMapper.readTree(nextTheoryResponse).get("id").asLong();

        String nextPracticeResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Next Practice",
                                  "description": "Should be blocked until first completed",
                                  "lessonType": "PRACTICE_TEST",
                                  "fullPoints": 2,
                                  "partialPoints": 0,
                                  "passingThresholdPercent": 100,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "2 + 2 = ?",
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
        Long nextPracticeId = objectMapper.readTree(nextPracticeResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", nextTheoryId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", nextPracticeId)
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

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", blockingTheoryId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/complete-theory", blockingTheoryId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", nextTheoryId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", nextPracticeId)
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
    }

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
                                  "lessonType": "THEORY_TEXT",
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

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", theoryLessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        List<LessonSubmission> autoTheorySubmissions = lessonSubmissionRepository.findAll().stream()
                .filter(s -> s.getLesson().getId().equals(theoryLessonId) && s.getStudent().getId().equals(studentId))
                .toList();
        assertThat(autoTheorySubmissions).isEmpty();

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/complete-theory", theoryLessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        List<LessonSubmission> afterManualComplete = lessonSubmissionRepository.findAll().stream()
                .filter(s -> s.getLesson().getId().equals(theoryLessonId) && s.getStudent().getId().equals(studentId))
                .toList();
        assertThat(afterManualComplete).hasSize(1);

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
                                  "lessonType": "THEORY_TEXT",
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

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d, %d]
                                }
                                """.formatted(studentDoneId, studentNewId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", theoryLessonId)
                        .header("Authorization", "Bearer " + studentDoneToken))
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
    void course_summary_report_csv_should_match_required_columns_and_stats() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Course Report FR-017",
                                  "description": "AC-017 checks",
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
                                  "title": "Report Theory",
                                  "description": "One lesson",
                                  "lessonType": "THEORY_TEXT",
                                  "content": "Theory",
                                  "fullPoints": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long theoryLessonId = objectMapper.readTree(theoryLessonResponse).get("id").asLong();

        String studentDoneEmail = uniqueEmail("report-done");
        Long studentDoneId = createUser(adminToken, "Report Done", studentDoneEmail, "STUDENT");
        String studentDoneToken = setPasswordAndLogin(studentDoneId, studentDoneEmail, "Stud123!");

        String studentNewEmail = uniqueEmail("report-new");
        Long studentNewId = createUser(adminToken, "Report New", studentNewEmail, "STUDENT");
        setPasswordAndLogin(studentNewId, studentNewEmail, "Stud123!");

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d, %d]
                                }
                                """.formatted(studentDoneId, studentNewId)))
                .andExpect(status().isOk());

        String courseStatsResponse = mockMvc.perform(get("/api/v1/admin/progress/courses/{courseId}/stats", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode courseStats = objectMapper.readTree(courseStatsResponse);

        String summaryCsv = mockMvc.perform(get("/api/v1/admin/progress/courses/{courseId}/summary-report.csv", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String[] lines = summaryCsv.strip().split("\\R");
        assertThat(lines.length).isGreaterThanOrEqualTo(3);

        String expectedHeader =
                "\"Статус\";\"Программа\";\"ФИО\";\"Email\";\"Логин\";\"cid\";\"Деактивирован\";\"Компания\";\"Подразделение\";\"Должность\";\"Группы\";\"Баллов\";\"Эффективность\";\"Медалей\";\"Пересдач\";\"Назначено\";\"Начало\";\"Завершение\";\"Дедлайн\";\"Затрачено времени\";\"Прогресс\";\"Уроков\";\"Продолжительность\";\"Номер сертификата\";\"Ссылка\"";
        assertThat(lines[0]).isEqualTo(expectedHeader);

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
        assertThat(newCourseStat).isNotNull();

        String[] row1 = parseCsvSemicolonLine(lines[1]);
        String[] row2 = parseCsvSemicolonLine(lines[2]);
        assertThat(row1.length).isEqualTo(25);
        assertThat(row2.length).isEqualTo(25);

        String[] doneRow = row1[2].equals("Report Done") ? row1 : row2;
        String[] newRow = row1[2].equals("Report New") ? row1 : row2;

        assertThat(doneRow[11]).isEqualTo(String.valueOf(doneCourseStat.get("earnedPoints").asInt()));
        assertThat(doneRow[12]).isEqualTo(String.format(java.util.Locale.US, "%.2f", doneCourseStat.get("efficiencyPercent").asDouble()));
        assertThat(doneRow[20]).isEqualTo(String.format(java.util.Locale.US, "%.2f%%", doneCourseStat.get("progressPercent").asDouble()));
        assertThat(doneRow[21]).isEqualTo(doneCourseStat.get("completedLessons").asText() + "/" + doneCourseStat.get("totalLessons").asText());

        assertThat(newRow[11]).isEqualTo(String.valueOf(newCourseStat.get("earnedPoints").asInt()));
        assertThat(newRow[12]).isEqualTo(String.format(java.util.Locale.US, "%.2f", newCourseStat.get("efficiencyPercent").asDouble()));
        assertThat(newRow[20]).isEqualTo(String.format(java.util.Locale.US, "%.2f%%", newCourseStat.get("progressPercent").asDouble()));
        assertThat(newRow[21]).isEqualTo(newCourseStat.get("completedLessons").asText() + "/" + newCourseStat.get("totalLessons").asText());

        assertThat(doneRow[4]).isEmpty();
        assertThat(doneRow[5]).isEmpty();
        assertThat(doneRow[13]).isEmpty();
        assertThat(doneRow[23]).isEmpty();
        assertThat(doneRow[24]).isEmpty();
    }

    @Test
    void summary_report_csv_should_match_required_columns_and_have_row_per_course_assignment() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("summary-multi");
        Long studentId = createUser(adminToken, "Summary Multi", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String firstCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Summary Course A",
                                  "description": "FR-017 all courses A",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 70,
                                  "deadlineDays": 30
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long firstCourseId = objectMapper.readTree(firstCourseResponse).get("id").asLong();

        String secondCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Summary Course B",
                                  "description": "FR-017 all courses B",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 70,
                                  "deadlineDays": 45
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long secondCourseId = objectMapper.readTree(secondCourseResponse).get("id").asLong();

        Long firstTheoryLessonId = objectMapper.readTree(mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/theory", firstCourseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Theory A",
                                  "description": "A",
                                  "lessonType": "THEORY_TEXT",
                                  "content": "A",
                                  "fullPoints": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/theory", secondCourseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Theory B",
                                  "description": "B",
                                  "lessonType": "THEORY_TEXT",
                                  "content": "B",
                                  "fullPoints": 20
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", firstCourseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", secondCourseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", firstTheoryLessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/complete-theory", firstTheoryLessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        String summaryCsv = mockMvc.perform(get("/api/v1/admin/progress/reports/summary.csv")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String[] lines = summaryCsv.strip().split("\\R");
        assertThat(lines.length).isGreaterThanOrEqualTo(2);

        String expectedHeader =
                "\"Группы\";\"ФИО студента\";\"Email\";\"Логин\";\"CID\";\"Название курса\";\"CID\";\"Дата назначения\";\"Время\";\"Дата начала\";\"Время\";\"Дата завершения\";\"Время\";\"Баллов\";\"Эффективность\";\"Продолжительность\";\"Затрачено\";\"Номер сертификата\";\"Ссылка\"";
        assertThat(lines[0]).isEqualTo(expectedHeader);

        java.util.List<String[]> studentRows = new java.util.ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String[] row = parseCsvSemicolonLine(lines[i]);
            if (row.length == 19 && studentEmail.equals(row[2])) {
                studentRows.add(row);
            }
        }

        assertThat(studentRows).hasSize(2);

        String[] courseARow = studentRows.stream()
                .filter(r -> "Summary Course A".equals(r[5]))
                .findFirst()
                .orElseThrow();
        String[] courseBRow = studentRows.stream()
                .filter(r -> "Summary Course B".equals(r[5]))
                .findFirst()
                .orElseThrow();

        assertThat(courseARow[1]).isEqualTo("Summary Multi");
        assertThat(courseARow[2]).isEqualTo(studentEmail);
        assertThat(courseARow[13]).isEqualTo("10");
        assertThat(courseARow[14]).isEqualTo("100.00");

        assertThat(courseBRow[1]).isEqualTo("Summary Multi");
        assertThat(courseBRow[2]).isEqualTo(studentEmail);
        assertThat(courseBRow[13]).isEqualTo("0");
        assertThat(courseBRow[14]).isEqualTo("0.00");

        assertThat(courseARow[3]).isEmpty();
        assertThat(courseARow[4]).isEmpty();
        assertThat(courseARow[6]).isEmpty();
        assertThat(courseARow[17]).isEmpty();
        assertThat(courseARow[18]).isEmpty();
    }

    @Test
    void enrollment_two_lists_flow_should_work() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String enrolledCandidateEmail = uniqueEmail("in-candidate");
        Long enrolledCandidateId = createUser(adminToken, "Enrolled Candidate", enrolledCandidateEmail, "STUDENT");
        String enrolledCandidateToken = setPasswordAndLogin(enrolledCandidateId, enrolledCandidateEmail, "Stud123!");

        String notEnrolledCandidateEmail = uniqueEmail("not-in-candidate");
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

        String initialListsResponse = mockMvc.perform(get("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode initialLists = objectMapper.readTree(initialListsResponse);
        assertThat(initialLists.get("in").isEmpty()).isTrue();
        assertThat(initialLists.get("notIn").toString()).contains("\"id\":" + enrolledCandidateId);
        assertThat(initialLists.get("notIn").toString()).contains("\"id\":" + notEnrolledCandidateId);

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(enrolledCandidateId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + enrolledCandidateToken))
                .andExpect(status().isOk());

        String afterEnrollListsResponse = mockMvc.perform(get("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode afterEnrollLists = objectMapper.readTree(afterEnrollListsResponse);
        assertThat(afterEnrollLists.get("in").toString()).contains("\"id\":" + enrolledCandidateId);
        assertThat(afterEnrollLists.get("notIn").toString()).doesNotContain("\"id\":" + enrolledCandidateId);

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsNotIn": [%d]
                                }
                                """.formatted(enrolledCandidateId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + enrolledCandidateToken))
                .andExpect(status().isBadRequest());

        String afterUnenrollListsResponse = mockMvc.perform(get("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode afterUnenrollLists = objectMapper.readTree(afterUnenrollListsResponse);
        assertThat(afterUnenrollLists.get("in").toString()).doesNotContain("\"id\":" + enrolledCandidateId);
        assertThat(afterUnenrollLists.get("notIn").toString()).contains("\"id\":" + enrolledCandidateId);
    }

    @Test
    void learner_course_progress_and_next_lesson_endpoint_should_return_expected_data() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("learner-progress");
        Long studentId = createUser(adminToken, "Learner Progress", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Learner Progress Course",
                                  "description": "Learner progress checks",
                                  "authorFullName": "Admin",
                                  "lessonsFreeOrder": false,
                                  "deadlineDays": 30
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long courseId = objectMapper.readTree(createCourseResponse).get("id").asLong();

        String firstTheoryResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/theory", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Theory 1",
                                  "description": "First",
                                  "lessonType": "THEORY_TEXT",
                                  "content": "First content",
                                  "fullPoints": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long firstTheoryId = objectMapper.readTree(firstTheoryResponse).get("id").asLong();

        String secondTheoryResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/theory", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Theory 2",
                                  "description": "Second",
                                  "lessonType": "THEORY_TEXT",
                                  "content": "Second content",
                                  "fullPoints": 6
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long secondTheoryId = objectMapper.readTree(secondTheoryResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        String learnerCourseBefore = mockMvc.perform(get("/api/v1/student/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode learnerCourseBeforeNode = objectMapper.readTree(learnerCourseBefore);

        assertThat(learnerCourseBeforeNode.get("completionPercent").asInt()).isEqualTo(0);
        assertThat(learnerCourseBeforeNode.get("completedLessons").asInt()).isEqualTo(0);
        assertThat(learnerCourseBeforeNode.get("remainingLessons").asInt()).isEqualTo(2);
        assertThat(learnerCourseBeforeNode.get("totalLessons").asInt()).isEqualTo(2);
        assertThat(learnerCourseBeforeNode.get("courseCompleted").asBoolean()).isFalse();

        JsonNode lessonsBefore = learnerCourseBeforeNode.get("lessons");
        assertThat(lessonsBefore.size()).isEqualTo(2);
        assertThat(lessonsBefore.get(0).get("id").asLong()).isEqualTo(firstTheoryId);
        assertThat(lessonsBefore.get(0).get("completed").asBoolean()).isFalse();
        assertThat(lessonsBefore.get(0).get("blocked").asBoolean()).isFalse();
        assertThat(lessonsBefore.get(0).get("pointsAwarded").asInt()).isEqualTo(0);

        assertThat(lessonsBefore.get(1).get("id").asLong()).isEqualTo(secondTheoryId);
        assertThat(lessonsBefore.get(1).get("completed").asBoolean()).isFalse();
        assertThat(lessonsBefore.get(1).get("blocked").asBoolean()).isTrue();
        assertThat(lessonsBefore.get(1).get("blockReason").asText()).isEqualTo("PREVIOUS_LESSON_NOT_PASSED");

        String nextLessonBefore = mockMvc.perform(get("/api/v1/student/courses/{courseId}/lessons/next", courseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(nextLessonBefore).get("id").asLong()).isEqualTo(firstTheoryId);

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/complete-theory", firstTheoryId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        String learnerCourseMid = mockMvc.perform(get("/api/v1/student/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode learnerCourseMidNode = objectMapper.readTree(learnerCourseMid);

        assertThat(learnerCourseMidNode.get("completionPercent").asInt()).isEqualTo(50);
        assertThat(learnerCourseMidNode.get("completedLessons").asInt()).isEqualTo(1);
        assertThat(learnerCourseMidNode.get("remainingLessons").asInt()).isEqualTo(1);
        assertThat(learnerCourseMidNode.get("courseCompleted").asBoolean()).isFalse();

        JsonNode lessonsMid = learnerCourseMidNode.get("lessons");
        assertThat(lessonsMid.get(0).get("completed").asBoolean()).isTrue();
        assertThat(lessonsMid.get(0).get("pointsAwarded").asInt()).isEqualTo(5);
        assertThat(lessonsMid.get(0).get("blocked").asBoolean()).isFalse();
        assertThat(lessonsMid.get(1).get("blocked").asBoolean()).isFalse();

        String nextLessonMid = mockMvc.perform(get("/api/v1/student/courses/{courseId}/lessons/next", courseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(nextLessonMid).get("id").asLong()).isEqualTo(secondTheoryId);

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/complete-theory", secondTheoryId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        String learnerCourseAfter = mockMvc.perform(get("/api/v1/student/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode learnerCourseAfterNode = objectMapper.readTree(learnerCourseAfter);

        assertThat(learnerCourseAfterNode.get("completionPercent").asInt()).isEqualTo(100);
        assertThat(learnerCourseAfterNode.get("completedLessons").asInt()).isEqualTo(2);
        assertThat(learnerCourseAfterNode.get("remainingLessons").asInt()).isEqualTo(0);
        assertThat(learnerCourseAfterNode.get("courseCompleted").asBoolean()).isTrue();

        mockMvc.perform(get("/api/v1/student/courses/{courseId}/lessons/next", courseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void practice_lesson_should_apply_partial_scoring_and_binary_lesson_points() throws Exception {
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

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        String practiceLessonResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Practice All Types",
                                  "description": "Partial scoring and binary lesson points",
                                  "lessonType": "PRACTICE_TEST",
                                  "passingThresholdPercent": 60,
                                  "fullPoints": 2,
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
                                      "options": ["2", "3", "4", "5"],
                                      "correctAnswers": ["2", "3"],
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

        String submitPracticeResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", practiceLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["4"],
                                    "2": ["2", "4"]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode submitPracticeResult = objectMapper.readTree(submitPracticeResponse);
        assertThat(submitPracticeResult.get("status").asText()).isEqualTo("COMPLETE");
        assertThat(submitPracticeResult.get("completed").asBoolean()).isTrue();

        LessonSubmission submission = lessonSubmissionRepository.findAll().stream()
                .filter(s -> s.getLesson().getId().equals(practiceLessonId) && s.getStudent().getId().equals(studentId))
                .reduce((a, b) -> b)
                .orElseThrow();

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.COMPLETE);
        assertThat(submission.getCompleted()).isTrue();
        assertThat(submission.getPointsAwarded()).isEqualTo(2);

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
        assertThat(courseStat.get("earnedPoints").asInt()).isEqualTo(2);
        assertThat(courseStat.get("maxPoints").asInt()).isEqualTo(2);
        assertThat(courseStat.get("efficiencyPercent").asDouble()).isEqualTo(100.0);
        assertThat(courseStat.get("progressPercent").asDouble()).isEqualTo(100.0);
        assertThat(courseStat.get("completedLessons").asInt()).isEqualTo(1);
        assertThat(courseStat.get("totalLessons").asInt()).isEqualTo(1);
    }

    @Test
    void learner_practice_questions_should_return_all_questions_and_shuffle_on_every_attempt() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("random-shuffle-student");
        Long studentId = createUser(adminToken, "Random Shuffle Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Random Question Count Course",
                                  "description": "FR-113 checks",
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

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        String practiceLessonResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Randomized Practice",
                                  "description": "FR-113",
                                  "lessonType": "PRACTICE_TEST",
                                  "shuffleOptions": true,
                                  "passingThresholdPercent": 60,
                                  "fullPoints": 1,
                                  "partialPoints": 0,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "Q1",
                                      "options": ["A", "B"],
                                      "correctAnswers": ["A"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 2,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "Q2",
                                      "options": ["A", "B"],
                                      "correctAnswers": ["A"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 3,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "Q3",
                                      "options": ["A", "B"],
                                      "correctAnswers": ["A"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 4,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "Q4",
                                      "options": ["A", "B"],
                                      "correctAnswers": ["A"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 5,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "Q5",
                                      "options": ["A", "B"],
                                      "correctAnswers": ["A"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 6,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "Q6",
                                      "options": ["A", "B"],
                                      "correctAnswers": ["A"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 7,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "Q7",
                                      "options": ["A", "B"],
                                      "correctAnswers": ["A"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 8,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "Q8",
                                      "options": ["A", "B"],
                                      "correctAnswers": ["A"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 9,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "Q9",
                                      "options": ["A", "B"],
                                      "correctAnswers": ["A"],
                                      "fullPoints": 1
                                    },
                                    {
                                      "position": 10,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "Q10",
                                      "options": ["A", "B"],
                                      "correctAnswers": ["A"],
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

        Set<String> attemptSignatures = new HashSet<>();
        for (int i = 0; i < 12; i++) {
            String learnerLessonResponse = mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", practiceLessonId)
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            JsonNode learnerLesson = objectMapper.readTree(learnerLessonResponse);
            JsonNode questions = learnerLesson.get("questions");
            assertThat(questions.size()).isEqualTo(10);

            List<Integer> indexes = new ArrayList<>();
            for (JsonNode question : questions) {
                int questionIndex = question.get("position").asInt();
                indexes.add(questionIndex);
                assertThat(questionIndex).isBetween(1, 10);
            }

            assertThat(new HashSet<>(indexes)).hasSize(10);
            attemptSignatures.add(indexes.toString());
        }

        assertThat(attemptSignatures.size()).isGreaterThan(1);
    }

    @Test
    void practice_attempt_limit_should_block_third_attempt_after_two_failed() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("attempt-limit-student");
        Long studentId = createUser(adminToken, "Attempt Limit Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Attempt Limit Course",
                                  "description": "FR-110/FR-111 checks",
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

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        String practiceLessonResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Attempt Limit Practice",
                                  "description": "Limited attempts",
                                  "lessonType": "PRACTICE_TEST",
                                  "attemptLimit": 2,
                                  "passingThresholdPercent": 100,
                                  "fullPoints": 1,
                                  "partialPoints": 0,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "2 + 2 = ?",
                                      "options": ["3", "4"],
                                      "correctAnswers": ["4"],
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

        String failedAttemptPayload = """
                {
                  "questionAnswers": {
                    "1": ["3"]
                  }
                }
                """;

        String firstAttemptResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", practiceLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failedAttemptPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondAttemptResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", practiceLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failedAttemptPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode firstAttempt = objectMapper.readTree(firstAttemptResponse);
        JsonNode secondAttempt = objectMapper.readTree(secondAttemptResponse);
        assertThat(firstAttempt.get("status").asText()).isEqualTo("INCOMPLETE");
        assertThat(firstAttempt.get("completed").asBoolean()).isFalse();
        assertThat(secondAttempt.get("status").asText()).isEqualTo("INCOMPLETE");
        assertThat(secondAttempt.get("completed").asBoolean()).isFalse();

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", practiceLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failedAttemptPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void time_limits_should_block_after_course_deadline_and_practice_time_limit() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("time-limit-student");
        Long studentId = createUser(adminToken, "Time Limit Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String createDeadlineCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Deadline Course",
                                  "description": "FR-112 deadlineDays check",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 70,
                                  "deadlineDays": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long deadlineCourseId = objectMapper.readTree(createDeadlineCourseResponse).get("id").asLong();

        String theoryLessonResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/theory", deadlineCourseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Deadline Theory",
                                  "description": "Theory",
                                  "lessonType": "THEORY_TEXT",
                                  "content": "Theory",
                                  "fullPoints": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long theoryLessonId = objectMapper.readTree(theoryLessonResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", deadlineCourseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        var enrollment = enrollmentRepository.findByUserIdAndCourseId(studentId, deadlineCourseId).orElseThrow();
        enrollment.setEnrolledAt(LocalDateTime.now().minusDays(2));
        enrollmentRepository.save(enrollment);

        mockMvc.perform(get("/api/v1/student/courses/{courseId}", deadlineCourseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());

        String createPracticeCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Practice Time Limit Course",
                                  "description": "FR-112 timeLimitMinutes check",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 70,
                                  "deadlineDays": 30
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long practiceCourseId = objectMapper.readTree(createPracticeCourseResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", practiceCourseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        String practiceLessonResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", practiceCourseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Practice Time Limit",
                                  "description": "Limited by time",
                                  "lessonType": "PRACTICE_TEST",
                                  "timeLimitMinutes": 1,
                                  "passingThresholdPercent": 100,
                                  "fullPoints": 1,
                                  "partialPoints": 0,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "2 + 2 = ?",
                                      "options": ["3", "4"],
                                      "correctAnswers": ["4"],
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

        String successfulAttemptPayload = """
                {
                  "questionAnswers": {
                    "1": ["4"]
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", practiceLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(successfulAttemptPayload))
                .andExpect(status().isOk());

        LessonSubmission firstAttempt = lessonSubmissionRepository.findAll().stream()
                .filter(s -> s.getLesson().getId().equals(practiceLessonId) && s.getStudent().getId().equals(studentId))
                .findFirst()
                .orElseThrow();
        firstAttempt.setFirstSubmittedAt(LocalDateTime.now().minusMinutes(2));
        lessonSubmissionRepository.save(firstAttempt);

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", practiceLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(successfulAttemptPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void learner_should_be_able_to_view_already_passed_lesson_after_deadline() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("view-completed-after-deadline");
        Long studentId = createUser(adminToken, "View Passed Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "View Passed After Deadline Course",
                                  "description": "view checks",
                                  "authorFullName": "Admin",
                                  "lessonsFreeOrder": false,
                                  "deadlineDays": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long courseId = objectMapper.readTree(createCourseResponse).get("id").asLong();

        String firstTheoryResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/theory", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Theory 1",
                                  "description": "First",
                                  "lessonType": "THEORY_TEXT",
                                  "content": "First content",
                                  "fullPoints": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long firstTheoryId = objectMapper.readTree(firstTheoryResponse).get("id").asLong();

        String secondTheoryResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/theory", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Theory 2",
                                  "description": "Second",
                                  "lessonType": "THEORY_TEXT",
                                  "content": "Second content",
                                  "fullPoints": 6
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long secondTheoryId = objectMapper.readTree(secondTheoryResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/complete-theory", firstTheoryId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        var enrollment = enrollmentRepository.findByUserIdAndCourseId(studentId, courseId).orElseThrow();
        enrollment.setEnrolledAt(LocalDateTime.now().minusDays(2));
        enrollmentRepository.save(enrollment);

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", firstTheoryId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", secondTheoryId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());
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
                                  "lessonType": "THEORY_TEXT",
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

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d, %d]
                                }
                                """.formatted(studentId, adminLearnerId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", theoryLessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/complete-theory", theoryLessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/student/lessons/{lessonId}", practiceLessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", practiceLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["4"],
                                    "2": ["2", "5"]
                                  }
                                }
                                """))
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
                                  "questionAnswers": {
                                    "1": ["My open answer"]
                                  }
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
                                  "idsIn": [%d]
                                }
                                """.formatted(adminLearnerId)))
                .andExpect(status().isOk());

        String reReviewResponse = mockMvc.perform(post("/api/v1/admin/progress/reviews/{submissionId}", openSubmissionId)
                        .header("Authorization", "Bearer " + adminLearnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionReviews": {
                                    "1": {
                                      "submissionStatus": "REWORK",
                                      "awardedPoints": 0,
                                      "reviewComment": "Need second pass"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode reReview = objectMapper.readTree(reReviewResponse);
        assertThat(reReview.get("status").asText()).isEqualTo("REWORK");
        assertThat(reReview.get("completed").asBoolean()).isFalse();

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
                                  "partialPoints": 1,
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
        assertThat(updatedPractice.get("questions").get(0).get("position").asInt()).isEqualTo(1);
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
                                  "lessonType": "THEORY_TEXT",
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
                                  "idsIn": [%d]
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

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
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
                                  "questionAnswers": {}
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", testPracticeLessonId)
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
                                  "questionAnswers": {}
                                }
                                """))
                .andExpect(status().isBadRequest());

        String openSubmissionResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", openPracticeLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["My detailed answer"]
                                  }
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
                                  "questionReviews": {
                                    "1": {
                                      "submissionStatus": "ACCEPTED",
                                      "awardedPoints": 3,
                                      "reviewComment": "Try"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/reviewers", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
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
                                  "questionReviews": {
                                    "1": {
                                      "submissionStatus": "REWORK",
                                      "awardedPoints": 0,
                                      "reviewComment": "Need student rework"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        String pendingAfterReworkResponse = mockMvc.perform(get("/api/v1/admin/progress/reviews/pending")
                        .header("Authorization", "Bearer " + otherAdminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode pendingAfterRework = objectMapper.readTree(pendingAfterReworkResponse);
        assertThat(pendingAfterRework).isEmpty();

        String resubmissionResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", openPracticeLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["My detailed answer v2"]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long resubmissionId = objectMapper.readTree(resubmissionResponse).get("submissionId").asLong();
        assertThat(resubmissionId).isEqualTo(openSubmissionId);

        String pendingAfterResubmitResponse = mockMvc.perform(get("/api/v1/admin/progress/reviews/pending")
                        .header("Authorization", "Bearer " + otherAdminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode pendingAfterResubmit = objectMapper.readTree(pendingAfterResubmitResponse);
        assertThat(pendingAfterResubmit.size()).isEqualTo(1);
        assertThat(pendingAfterResubmit.get(0).get("submissionId").asLong()).isEqualTo(openSubmissionId);

        mockMvc.perform(post("/api/v1/admin/progress/reviews/{submissionId}", openSubmissionId)
                        .header("Authorization", "Bearer " + otherAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionReviews": {
                                    "1": {
                                      "submissionStatus": "ACCEPTED",
                                      "awardedPoints": 3,
                                      "reviewComment": "Approved after resubmit"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", openPracticeLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["Brand new answer"]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/admin/progress/reviews/{submissionId}", openSubmissionId)
                        .header("Authorization", "Bearer " + otherAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionReviews": {
                                    "1": {
                                      "submissionStatus": "ACCEPTED",
                                      "awardedPoints": 3,
                                      "reviewComment": "Second review should fail"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void open_practice_with_multiple_questions_should_require_full_question_answers_and_be_reviewed_as_single_submission() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("multi-open-student");
        Long studentId = createUser(adminToken, "Multi Open Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String reviewerEmail = uniqueEmail("multi-open-reviewer");
        Long reviewerId = createUser(adminToken, "Multi Open Reviewer", reviewerEmail, "ADMIN");
        String reviewerToken = setPasswordAndLogin(reviewerId, reviewerEmail, "Admin123!");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Multi Open Course",
                                  "description": "Multiple open questions",
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

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        String openPracticeResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Open Practice Multi",
                                  "description": "Two open questions",
                                  "lessonType": "PRACTICE_OPEN_ANSWER",
                                  "fullPoints": 8,
                                  "partialPoints": 3,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "OPEN_ANSWER",
                                      "questionText": "Explain JVM",
                                      "trainerHint": "Details",
                                      "fullPoints": 4,
                                      "partialPoints": 2
                                    },
                                    {
                                      "position": 2,
                                      "questionType": "OPEN_ANSWER",
                                      "questionText": "Explain JIT",
                                      "trainerHint": "Details",
                                      "fullPoints": 4,
                                      "partialPoints": 1
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
                                  "questionAnswers": {
                                    "1": ["Only first answer"]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());

        String submissionResponse = mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/submit-practice", openPracticeLessonId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionAnswers": {
                                    "1": ["First open answer"],
                                    "2": ["Second open answer"]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long submissionId = objectMapper.readTree(submissionResponse).get("submissionId").asLong();

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/reviewers", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(reviewerId)))
                .andExpect(status().isOk());

        String reviewResponse = mockMvc.perform(post("/api/v1/admin/progress/reviews/{submissionId}", submissionId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionReviews": {
                                    "1": {
                                      "submissionStatus": "ACCEPTED",
                                      "awardedPoints": 4,
                                      "reviewComment": "Approved"
                                    },
                                    "2": {
                                      "submissionStatus": "ACCEPTED",
                                      "awardedPoints": 4,
                                      "reviewComment": "Approved"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode review = objectMapper.readTree(reviewResponse);
        assertThat(review.get("status").asText()).isEqualTo("COMPLETE");
        assertThat(review.get("completed").asBoolean()).isTrue();
    }

    @Test
    void lesson_updates_should_preserve_unspecified_fields_for_theory_and_practice() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Patch Semantics Course",
                                  "description": "lesson patch checks",
                                  "authorFullName": "Admin"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long courseId = objectMapper.readTree(createCourseResponse).get("id").asLong();

        String theoryCreateResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/theory", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Theory Initial",
                                  "description": "Theory Description",
                                  "lessonType": "THEORY_TEXT",
                                  "content": "Theory Content",
                                  "fullPoints": 7,
                                  "stopLesson": true,
                                  "attemptLimit": 2,
                                  "timeLimitMinutes": 15
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode createdTheory = objectMapper.readTree(theoryCreateResponse);
        Long theoryLessonId = createdTheory.get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/courses/{courseId}/lessons/{lessonId}/theory", courseId, theoryLessonId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Theory Updated"
                                }
                                """))
                .andExpect(status().isOk());

        String theoryAfterUpdateResponse = mockMvc.perform(get("/api/v1/admin/courses/{courseId}/lessons/{lessonId}", courseId, theoryLessonId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode theoryAfterUpdate = objectMapper.readTree(theoryAfterUpdateResponse);
        assertThat(theoryAfterUpdate.get("title").asText()).isEqualTo("Theory Updated");
        assertThat(theoryAfterUpdate.get("description").asText()).isEqualTo(createdTheory.get("description").asText());
        assertThat(theoryAfterUpdate.get("theoryContent").asText()).isEqualTo(createdTheory.get("theoryContent").asText());
        assertThat(theoryAfterUpdate.get("fullPoints").asInt()).isEqualTo(createdTheory.get("fullPoints").asInt());
        assertThat(theoryAfterUpdate.get("stopLesson").asBoolean()).isEqualTo(createdTheory.get("stopLesson").asBoolean());
        assertThat(theoryAfterUpdate.get("blockedDuringAttempt")).isNull();
        assertThat(theoryAfterUpdate.get("attemptLimit").asInt()).isEqualTo(createdTheory.get("attemptLimit").asInt());
        assertThat(theoryAfterUpdate.get("timeLimitMinutes").asInt()).isEqualTo(createdTheory.get("timeLimitMinutes").asInt());

        String practiceCreateResponse = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/lessons/practice", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Practice Initial",
                                  "description": "Practice Description",
                                  "lessonType": "PRACTICE_TEST",
                                  "passingThresholdPercent": 80,
                                  "shuffleOptions": true,
                                  "showQuestionStatus": false,
                                  "showCorrectAnswers": true,
                                  "fullPoints": 1,
                                  "stopLesson": true,
                                  "attemptLimit": 3,
                                  "timeLimitMinutes": 10,
                                  "questions": [
                                    {
                                      "position": 1,
                                      "questionType": "SINGLE_CHOICE",
                                      "questionText": "Q1",
                                      "options": ["A", "B"],
                                      "correctAnswers": ["A"],
                                      "fullPoints": 2
                                    },
                                    {
                                      "position": 2,
                                      "questionType": "MULTIPLE_CHOICE",
                                      "questionText": "Q2",
                                      "options": ["A", "B", "C"],
                                      "correctAnswers": ["A", "C"],
                                      "fullPoints": 3,
                                      "partialPoints": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode createdPractice = objectMapper.readTree(practiceCreateResponse);
        Long practiceLessonId = createdPractice.get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/courses/{courseId}/lessons/{lessonId}/practice", courseId, practiceLessonId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Practice Updated"
                                }
                                """))
                .andExpect(status().isOk());

        String practiceAfterUpdateResponse = mockMvc.perform(get("/api/v1/admin/courses/{courseId}/lessons/{lessonId}", courseId, practiceLessonId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode practiceAfterUpdate = objectMapper.readTree(practiceAfterUpdateResponse);
        assertThat(practiceAfterUpdate.get("title").asText()).isEqualTo("Practice Updated");
        assertThat(practiceAfterUpdate.get("description").asText()).isEqualTo(createdPractice.get("description").asText());
        assertThat(practiceAfterUpdate.get("passingThresholdPercent").asInt()).isEqualTo(createdPractice.get("passingThresholdPercent").asInt());
        assertThat(practiceAfterUpdate.get("randomQuestionCount")).isNull();
        assertThat(practiceAfterUpdate.get("shuffleOnEveryAttempt").asBoolean()).isEqualTo(createdPractice.get("shuffleOnEveryAttempt").asBoolean());
        assertThat(practiceAfterUpdate.get("showCorrectAnswersAfterCompletion").asBoolean())
                .isEqualTo(createdPractice.get("showCorrectAnswersAfterCompletion").asBoolean());
        assertThat(practiceAfterUpdate.get("fullPoints").asInt()).isEqualTo(createdPractice.get("fullPoints").asInt());
        assertThat(practiceAfterUpdate.get("stopLesson").asBoolean()).isEqualTo(createdPractice.get("stopLesson").asBoolean());
        assertThat(practiceAfterUpdate.get("blockedDuringAttempt")).isNull();
        assertThat(practiceAfterUpdate.get("attemptLimit").asInt()).isEqualTo(createdPractice.get("attemptLimit").asInt());
        assertThat(practiceAfterUpdate.get("timeLimitMinutes").asInt()).isEqualTo(createdPractice.get("timeLimitMinutes").asInt());
        assertThat(practiceAfterUpdate.get("questions").size()).isEqualTo(createdPractice.get("questions").size());
        assertThat(practiceAfterUpdate.get("questions").get(0).get("questionText").asText())
                .isEqualTo(createdPractice.get("questions").get(0).get("questionText").asText());
        assertThat(practiceAfterUpdate.get("questions").get(1).get("questionText").asText())
                .isEqualTo(createdPractice.get("questions").get(1).get("questionText").asText());
    }

    @Test
    void admin_progress_reset_endpoint_should_clear_student_course_progress() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String studentEmail = uniqueEmail("reset-progress-student");
        Long studentId = createUser(adminToken, "Reset Progress Student", studentEmail, "STUDENT");
        String studentToken = setPasswordAndLogin(studentId, studentEmail, "Stud123!");

        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Reset Progress Course",
                                  "description": "reset progress checks",
                                  "authorFullName": "Admin",
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
                                  "title": "Reset Theory",
                                  "description": "Theory",
                                  "lessonType": "THEORY_TEXT",
                                  "content": "Theory content",
                                  "fullPoints": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long theoryLessonId = objectMapper.readTree(theoryLessonResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/enrollments", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/student/lessons/{lessonId}/complete-theory", theoryLessonId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        var progressBeforeReset = courseProgressRepository.findByUserIdAndCourseId(studentId, courseId).orElseThrow();
        progressBeforeReset.setStartedAt(LocalDateTime.now().minusDays(1));
        progressBeforeReset.setCompletedAt(LocalDateTime.now());
        progressBeforeReset.setStatus(CourseProgressStatus.COMPLETED);
        courseProgressRepository.save(progressBeforeReset);

        assertThat(lessonSubmissionRepository.findByStudentIdAndLessonCourseId(studentId, courseId)).isNotEmpty();

        String resetResponse = mockMvc.perform(post("/api/v1/admin/progress/users/reset")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "courseId": %d
                                }
                                """.formatted(studentId, courseId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(resetResponse).get("message").asText())
                .isEqualTo("Student progress has been cleared");

        assertThat(lessonSubmissionRepository.findByStudentIdAndLessonCourseId(studentId, courseId)).isEmpty();

        var progressAfterReset = courseProgressRepository.findByUserIdAndCourseId(studentId, courseId).orElseThrow();
        assertThat(progressAfterReset.getStartedAt()).isNull();
        assertThat(progressAfterReset.getCompletedAt()).isNull();
        assertThat(progressAfterReset.getStatus()).isEqualTo(CourseProgressStatus.NEW);
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

    private String[] parseCsvSemicolonLine(String line) {
        String content = line;
        if (content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);
        }
        return content.split("\";\"", -1);
    }
}
