package ru.just.monolithmvp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.just.monolithmvp.model.CourseProgressStatus;
import ru.just.monolithmvp.repository.CourseProgressRepository;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.repository.ProgramEnrollmentRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:group-management-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.profiles.active=h2"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GroupManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ProgramEnrollmentRepository programEnrollmentRepository;

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private LessonSubmissionRepository lessonSubmissionRepository;

    @Test
    void should_create_groups_and_enforce_single_typed_membership_per_user() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long studentId = createUser(adminToken, "group.student@example.com", "STUDENT");

        UUID companyAId = createGroup(adminToken, "Company A", "COMPANY");
        UUID companyBId = createGroup(adminToken, "Company B", "COMPANY");
        UUID generalId = createGroup(adminToken, "General Team", "GENERAL");

        mockMvc.perform(post("/api/v1/admin/groups/{groupId}/members", companyAId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userIds": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/groups/{groupId}/members", companyBId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userIds": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/admin/groups/{groupId}/members", generalId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userIds": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        String profileResponse = mockMvc.perform(get("/api/v1/student/my/profile")
                        .header("Authorization", "Bearer " + login("group.student@example.com", "Password1!")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode groups = objectMapper.readTree(profileResponse).get("groups");
        assertThat(groups.isArray()).isTrue();
        assertThat(groups.size()).isEqualTo(2);
    }

    @Test
    void should_update_group_and_reject_type_change_when_members_conflict() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long studentId = createUser(adminToken, "group.update@example.com", "STUDENT");

        UUID companyId = createGroup(adminToken, "Company Main", "COMPANY");
        UUID positionId = createGroup(adminToken, "Position Existing", "POSITION");

        mockMvc.perform(post("/api/v1/admin/groups/{groupId}/members", companyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userIds": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/groups/{groupId}/members", positionId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userIds": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/admin/groups/{groupId}", companyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Company Renamed",
                                  "type": "COMPANY"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/admin/groups/{groupId}", companyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Company To Position",
                                  "type": "POSITION"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_assign_course_and_program_to_group_and_auto_apply_for_new_member() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long studentAId = createUser(adminToken, "group.mass.studentA@example.com", "STUDENT");
        Long studentBId = createUser(adminToken, "group.mass.studentB@example.com", "STUDENT");
        Long studentCId = createUser(adminToken, "group.mass.studentC@example.com", "STUDENT");

        UUID groupId = createGroup(adminToken, "Mass Assign Group", "GENERAL");
        Long courseId = createCourse(adminToken, "Mass Assignment Course");
        Long programId = createProgram(adminToken, courseId);

        mockMvc.perform(post("/api/v1/admin/groups/{groupId}/members", groupId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userIds": [%d, %d]
                                }
                                """.formatted(studentAId, studentBId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/groups/assign", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": ["%s"]
                                }
                                """.formatted(groupId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/groups/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": ["%s"]
                                }
                                """.formatted(groupId)))
                .andExpect(status().isOk());

        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentAId, courseId)).isTrue();
        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentBId, courseId)).isTrue();
        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentAId, programId)).isTrue();
        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentBId, programId)).isTrue();

        mockMvc.perform(post("/api/v1/admin/groups/{groupId}/members", groupId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userIds": [%d]
                                }
                                """.formatted(studentCId)))
                .andExpect(status().isOk());

        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentCId, courseId)).isTrue();
        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentCId, programId)).isTrue();
    }

    @Test
    void should_unassign_course_even_if_user_remains_in_other_assigned_group() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long studentId = createUser(adminToken, "group.multi.student@example.com", "STUDENT");
        UUID groupAId = createGroup(adminToken, "Multi Group A", "GENERAL");
        UUID groupBId = createGroup(adminToken, "Multi Group B", "GENERAL");
        Long courseId = createCourse(adminToken, "Multi Group Course");

        mockMvc.perform(post("/api/v1/admin/groups/{groupId}/members", groupAId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userIds": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/groups/{groupId}/members", groupBId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userIds": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/groups/assign", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": ["%s", "%s"]
                                }
                                """.formatted(groupAId, groupBId)))
                .andExpect(status().isOk());

        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseId)).isTrue();
        assertThat(courseProgressRepository.existsByUserIdAndCourseId(studentId, courseId)).isTrue();

        var progress = courseProgressRepository.findByUserIdAndCourseId(studentId, courseId).orElseThrow();
        progress.setStartedAt(LocalDateTime.now(Clock.systemUTC()));
        progress.setStatus(CourseProgressStatus.IN_PROGRESS);
        courseProgressRepository.save(progress);

        mockMvc.perform(delete("/api/v1/admin/courses/{courseId}/groups/assign", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": ["%s"]
                                }
                                """.formatted(groupAId)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/admin/courses/{courseId}/groups/assign", courseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": ["%s"]
                                }
                                """.formatted(groupAId)))
                .andExpect(status().isOk());

        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseId)).isFalse();
        assertThat(courseProgressRepository.existsByUserIdAndCourseId(studentId, courseId)).isFalse();
        assertThat(lessonSubmissionRepository.findByStudentIdAndLessonCourseId(studentId, courseId)).isEmpty();
    }

    private Long createUser(String adminToken, String email, String role) throws Exception {
        String createUserResponse = mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Group Test User",
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

    private UUID createGroup(String adminToken, String title, String type) throws Exception {
        String createGroupResponse = mockMvc.perform(post("/api/v1/admin/groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "type": "%s"
                                }
                                """.formatted(title, type)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(objectMapper.readTree(createGroupResponse).get("id").asText());
    }

    private Long createCourse(String adminToken, String title) throws Exception {
        String createCourseResponse = mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Course for group assignment",
                                  "authorFullName": "Admin"
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(createCourseResponse).get("id").asLong();
    }

    private Long createProgram(String adminToken, Long courseId) throws Exception {
        String createProgramResponse = mockMvc.perform(post("/api/v1/admin/courses/programs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Group Program",
                                  "description": "Program for group assignment",
                                  "accessCondition": "ALL_OPEN",
                                  "blockAfterDeadline": false,
                                  "courses": [
                                    %d
                                  ]
                                }
                                """.formatted(courseId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(createProgramResponse).get("id").asLong();
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
