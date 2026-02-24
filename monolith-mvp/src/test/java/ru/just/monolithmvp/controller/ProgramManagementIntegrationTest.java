package ru.just.monolithmvp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.just.monolithmvp.model.Enrollment;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.ProgramEnrollmentRepository;
import ru.just.monolithmvp.service.ProgramService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:program-management-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.profiles.active=h2"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProgramManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProgramService programService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ProgramEnrollmentRepository programEnrollmentRepository;

    @Test
    void should_create_and_update_learning_program_with_ordered_courses() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long courseA = createCourse(adminToken, "Program Course A");
        Long courseB = createCourse(adminToken, "Program Course B");
        Long courseC = createCourse(adminToken, "Program Course C");

        String createProgramResponse = mockMvc.perform(post("/api/v1/admin/courses/programs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program Alpha",
                                  "description": "Initial order",
                                  "accessCondition": "ALL_OPEN",
                                  "blockAfterDeadline": false,
                                  "courses": [
                                    {"courseId": %d},
                                    {"courseId": %d},
                                    {"courseId": %d}
                                  ]
                                }
                                """.formatted(courseA, courseB, courseC)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdProgram = objectMapper.readTree(createProgramResponse);
        Long programId = createdProgram.get("id").asLong();

        assertThat(createdProgram.get("courses").size()).isEqualTo(3);
        assertThat(createdProgram.get("courses").get(0).get("courseId").asLong()).isEqualTo(courseA);
        assertThat(createdProgram.get("courses").get(1).get("courseId").asLong()).isEqualTo(courseB);
        assertThat(createdProgram.get("courses").get(2).get("courseId").asLong()).isEqualTo(courseC);

        mockMvc.perform(put("/api/v1/admin/courses/programs/{programId}", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program Alpha Updated",
                                  "description": "Reordered",
                                  "accessCondition": "ALL_OPEN",
                                  "blockAfterDeadline": false,
                                  "courses": [
                                    {"courseId": %d},
                                    {"courseId": %d},
                                    {"courseId": %d}
                                  ]
                                }
                                """.formatted(courseC, courseA, courseB)))
                .andExpect(status().isOk());

        String getProgramResponse = mockMvc.perform(get("/api/v1/admin/courses/programs/{programId}", programId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode updatedProgram = objectMapper.readTree(getProgramResponse);
        assertThat(updatedProgram.get("title").asText()).isEqualTo("Program Alpha Updated");
        assertThat(updatedProgram.get("courses").size()).isEqualTo(3);
        assertThat(updatedProgram.get("courses").get(0).get("courseId").asLong()).isEqualTo(courseC);
        assertThat(updatedProgram.get("courses").get(1).get("courseId").asLong()).isEqualTo(courseA);
        assertThat(updatedProgram.get("courses").get(2).get("courseId").asLong()).isEqualTo(courseB);

        String listProgramsResponse = mockMvc.perform(get("/api/v1/admin/courses/programs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode list = objectMapper.readTree(listProgramsResponse);
        JsonNode listedProgram = list.get(0);
        assertThat(listedProgram.get("id").asLong()).isEqualTo(programId);
        assertThat(listedProgram.get("courses").get(0).get("courseId").asLong()).isEqualTo(courseC);
        assertThat(listedProgram.get("courses").get(1).get("courseId").asLong()).isEqualTo(courseA);
        assertThat(listedProgram.get("courses").get(2).get("courseId").asLong()).isEqualTo(courseB);
    }

    @Test
    void should_apply_program_access_rules_and_deadline_blocking_for_student() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long courseA = createCourse(adminToken, "Rule Course A");
        Long courseB = createCourse(adminToken, "Rule Course B");
        Long studentId = createUser(adminToken, "program.rules.student@example.com", "STUDENT");

        String createProgramResponse = mockMvc.perform(post("/api/v1/admin/courses/programs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program Rules",
                                  "description": "Access + deadline rules",
                                  "accessCondition": "PREVIOUS_COURSES_COMPLETED",
                                  "deadlineAt": "%s",
                                  "blockAfterDeadline": true,
                                  "courses": [
                                    {"courseId": %d},
                                    {"courseId": %d}
                                  ]
                                }
                                """.formatted(LocalDateTime.now().plusDays(1), courseA, courseB)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long programId = objectMapper.readTree(createProgramResponse).get("id").asLong();
        programService.assignUsersToProgram(programId, java.util.List.of(studentId));

        JsonNode initialProgram = objectMapper.valueToTree(programService.getMyProgram(studentId, programId));
        assertThat(initialProgram.get("courses").get(0).get("available").asBoolean()).isTrue();
        assertThat(initialProgram.get("courses").get(1).get("available").asBoolean()).isFalse();

        Enrollment firstEnrollment = enrollmentRepository.findByUserIdAndCourseId(studentId, courseA).orElseThrow();
        firstEnrollment.setCompletedAt(LocalDateTime.now());
        enrollmentRepository.save(firstEnrollment);

        JsonNode afterCompletionProgram = objectMapper.valueToTree(programService.getMyProgram(studentId, programId));
        assertThat(afterCompletionProgram.get("courses").get(1).get("available").asBoolean()).isTrue();

        mockMvc.perform(put("/api/v1/admin/courses/programs/{programId}", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program Rules",
                                  "description": "Access + deadline rules",
                                  "accessCondition": "PREVIOUS_COURSES_COMPLETED",
                                  "deadlineAt": "%s",
                                  "blockAfterDeadline": true,
                                  "courses": [
                                    {"courseId": %d},
                                    {"courseId": %d}
                                  ]
                                }
                                """.formatted(LocalDateTime.now().minusDays(1), courseA, courseB)))
                .andExpect(status().isOk());

        JsonNode afterDeadlineProgram = objectMapper.valueToTree(programService.getMyProgram(studentId, programId));
        assertThat(afterDeadlineProgram.get("courses").get(0).get("available").asBoolean()).isTrue();
        assertThat(afterDeadlineProgram.get("courses").get(1).get("available").asBoolean()).isFalse();
    }

    @Test
    void should_assign_program_to_user_and_group_and_expose_in_student_cabinet() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long courseA = createCourse(adminToken, "Assign Program Course A");
        Long courseB = createCourse(adminToken, "Assign Program Course B");

        Long studentDirectId = createUser(adminToken, "program.assign.direct@example.com", "STUDENT");
        Long studentGroupAId = createUser(adminToken, "program.assign.groupA@example.com", "STUDENT");
        Long studentGroupBId = createUser(adminToken, "program.assign.groupB@example.com", "STUDENT");

        String createProgramResponse = mockMvc.perform(post("/api/v1/admin/courses/programs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program Assignments",
                                  "description": "FR-105",
                                  "accessCondition": "ALL_OPEN",
                                  "blockAfterDeadline": false,
                                  "courses": [
                                    {"courseId": %d},
                                    {"courseId": %d}
                                  ]
                                }
                                """.formatted(courseA, courseB)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long programId = objectMapper.readTree(createProgramResponse).get("id").asLong();

        String createGroupResponse = mockMvc.perform(post("/api/v1/admin/groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program Assign Group",
                                  "type": "GENERAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String groupId = objectMapper.readTree(createGroupResponse).get("id").asText();

        mockMvc.perform(post("/api/v1/admin/groups/{groupId}/members", groupId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userIds": [%d, %d]
                                }
                                """.formatted(studentGroupAId, studentGroupBId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [%d]
                                }
                                """.formatted(studentDirectId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/groups/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": ["%s"]
                                }
                                """.formatted(groupId)))
                .andExpect(status().isOk());

        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentDirectId, programId)).isTrue();
        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentGroupAId, programId)).isTrue();
        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentGroupBId, programId)).isTrue();

        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentDirectId, courseA)).isTrue();
        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentDirectId, courseB)).isTrue();
        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentGroupAId, courseA)).isTrue();
        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentGroupBId, courseB)).isTrue();

        String directStudentToken = login("program.assign.direct@example.com", "Password1!");
        String myProgramsResponse = mockMvc.perform(get("/api/v1/student/my/programs")
                        .header("Authorization", "Bearer " + directStudentToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode myPrograms = objectMapper.readTree(myProgramsResponse);
        assertThat(myPrograms.isArray()).isTrue();
        assertThat(myPrograms).anyMatch(node -> node.get("id").asLong() == programId);

        mockMvc.perform(get("/api/v1/student/my/programs/{programId}", programId)
                        .header("Authorization", "Bearer " + directStudentToken))
                .andExpect(status().isOk());
    }

    private Long createCourse(String adminToken, String title) throws Exception {
        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Course for program",
                                  "authorFullName": "Admin"
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(createCourseResponse).get("id").asLong();
    }

    private Long createUser(String adminToken, String email, String role) throws Exception {
        String createUserResponse = mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Program Rules User",
                                  "email": "%s",
                                  "role": "%s",
                                  "password": "Password1!"
                                }
                                """.formatted(email, role)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(createUserResponse).get("id").asLong();
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