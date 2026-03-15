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
import ru.just.monolithmvp.model.CourseProgress;
import ru.just.monolithmvp.model.CourseProgressStatus;
import ru.just.monolithmvp.repository.EnrollmentRepository;
import ru.just.monolithmvp.repository.CourseProgressRepository;
import ru.just.monolithmvp.repository.ProgramEnrollmentRepository;
import ru.just.monolithmvp.service.ProgramService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    private CourseProgressRepository courseProgressRepository;

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
                                    %d,
                                    %d,
                                    %d
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
                                    %d,
                                    %d,
                                    %d
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
        JsonNode listedProgram = null;
        for (JsonNode node : list) {
            if (node.get("id").asLong() == programId) {
                listedProgram = node;
                break;
            }
        }
        assertThat(listedProgram).isNotNull();
        assertThat(listedProgram.get("id").asLong()).isEqualTo(programId);
        assertThat(listedProgram.get("courses").get(0).get("courseId").asLong()).isEqualTo(courseC);
        assertThat(listedProgram.get("courses").get(1).get("courseId").asLong()).isEqualTo(courseA);
        assertThat(listedProgram.get("courses").get(2).get("courseId").asLong()).isEqualTo(courseB);
    }

    @Test
    void should_delete_learning_program() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long courseA = createCourse(adminToken, "Program Delete Course A");
        Long studentId = createUser(adminToken, "program.delete.student@example.com", "STUDENT");

        String createProgramResponse = mockMvc.perform(post("/api/v1/admin/courses/programs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program To Delete",
                                  "description": "Delete endpoint",
                                  "accessCondition": "ALL_OPEN",
                                  "blockAfterDeadline": false,
                                  "courses": [%d]
                                }
                                """.formatted(courseA)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long programId = objectMapper.readTree(createProgramResponse).get("id").asLong();
        programService.assignUsersToProgram(programId, java.util.List.of(studentId));
        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentId, programId)).isTrue();

        mockMvc.perform(delete("/api/v1/admin/courses/programs/{programId}", programId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/courses/programs/{programId}", programId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentId, programId)).isFalse();
    }

    @Test
    void should_support_assign_contract_with_idsIn_idsNotIn_and_backward_ids() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long courseA = createCourse(adminToken, "Assign Contract Course A");
        Long studentDirectA = createUser(adminToken, "program.assign.contract.a@example.com", "STUDENT");
        Long studentDirectB = createUser(adminToken, "program.assign.contract.b@example.com", "STUDENT");
        Long studentGroupOnly = createUser(adminToken, "program.assign.contract.group@example.com", "STUDENT");

        String createProgramResponse = mockMvc.perform(post("/api/v1/admin/courses/programs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program Assign Contract",
                                  "description": "idsIn/idsNotIn + ids",
                                  "accessCondition": "ALL_OPEN",
                                  "blockAfterDeadline": false,
                                  "courses": [
                                    %d
                                  ]
                                }
                                """.formatted(courseA)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long programId = objectMapper.readTree(createProgramResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d],
                                  "idsNotIn": []
                                }
                                """.formatted(studentDirectA)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d]
                                }
                                """.formatted(studentDirectB)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [],
                                  "idsNotIn": [%d]
                                }
                                """.formatted(studentDirectA)))
                .andExpect(status().isOk());

        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentDirectA, programId)).isFalse();
        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentDirectB, programId)).isTrue();

        String createGroupResponse = mockMvc.perform(post("/api/v1/admin/groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program Assign Contract Group",
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
                                  "userIds": [%d]
                                }
                                """.formatted(studentGroupOnly)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/groups/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": ["%s"],
                                  "idsNotIn": []
                                }
                                """.formatted(groupId)))
                .andExpect(status().isOk());

        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentGroupOnly, programId)).isTrue();

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/groups/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [],
                                  "idsNotIn": ["%s"]
                                }
                                """.formatted(groupId)))
                .andExpect(status().isOk());

        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentGroupOnly, programId)).isFalse();

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/groups/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": ["%s"]
                                }
                                """.formatted(groupId)))
                .andExpect(status().isOk());

        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentGroupOnly, programId)).isTrue();
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
                                    %d,
                                    %d
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

        CourseProgress firstProgress = courseProgressRepository.findByUserIdAndCourseId(studentId, courseA).orElseThrow();
        firstProgress.setCompletedAt(LocalDateTime.now());
        firstProgress.setStatus(CourseProgressStatus.COMPLETED);
        courseProgressRepository.save(firstProgress);

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
                                    %d,
                                    %d
                                  ]
                                }
                                """.formatted(LocalDateTime.now().minusDays(1), courseA, courseB)))
                .andExpect(status().isOk());

        JsonNode afterDeadlineProgram = objectMapper.valueToTree(programService.getMyProgram(studentId, programId));
        assertThat(afterDeadlineProgram.get("courses").get(0).get("available").asBoolean()).isTrue();
        assertThat(afterDeadlineProgram.get("courses").get(1).get("available").asBoolean()).isFalse();
    }

    @Test
    void previous_courses_completed_should_require_all_previous_courses_completed() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long courseA = createCourse(adminToken, "Rule All Previous Course A");
        Long courseB = createCourse(adminToken, "Rule All Previous Course B");
        Long courseC = createCourse(adminToken, "Rule All Previous Course C");
        Long studentId = createUser(adminToken, "program.rules.all.previous@example.com", "STUDENT");

        String createProgramResponse = mockMvc.perform(post("/api/v1/admin/courses/programs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program Rules All Previous",
                                  "description": "All previous must be completed",
                                  "accessCondition": "PREVIOUS_COURSES_COMPLETED",
                                  "blockAfterDeadline": false,
                                  "courses": [
                                    %d,
                                    %d,
                                    %d
                                  ]
                                }
                                """.formatted(courseA, courseB, courseC)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long programId = objectMapper.readTree(createProgramResponse).get("id").asLong();
        programService.assignUsersToProgram(programId, java.util.List.of(studentId));

        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseA)).isTrue();
        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseB)).isFalse();
        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseC)).isFalse();

        CourseProgress progressA = courseProgressRepository.findByUserIdAndCourseId(studentId, courseA).orElseThrow();
        progressA.setStatus(CourseProgressStatus.COMPLETED);
        progressA.setCompletedAt(LocalDateTime.now());
        courseProgressRepository.save(progressA);

        programService.onCourseProgressChanged(studentId, courseA);

        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseB)).isTrue();
        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseC)).isFalse();

        CourseProgress progressB = courseProgressRepository.findByUserIdAndCourseId(studentId, courseB).orElseThrow();
        progressB.setStatus(CourseProgressStatus.COMPLETED);
        progressB.setCompletedAt(LocalDateTime.now());
        courseProgressRepository.save(progressB);

        programService.onCourseProgressChanged(studentId, courseB);

        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseC)).isTrue();
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
                                    %d,
                                    %d
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
                                  "idsIn": [%d]
                                }
                                """.formatted(studentDirectId)))
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

    @Test
    void program_enrollment_two_lists_flow_should_work() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long courseA = createCourse(adminToken, "Program Lists Course A");
        Long studentA = createUser(adminToken, "program.lists.a@example.com", "STUDENT");
        Long studentB = createUser(adminToken, "program.lists.b@example.com", "STUDENT");

        String createProgramResponse = mockMvc.perform(post("/api/v1/admin/courses/programs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program Lists",
                                  "description": "Two-lists contract",
                                  "accessCondition": "ALL_OPEN",
                                  "blockAfterDeadline": false,
                                  "courses": [%d]
                                }
                                """.formatted(courseA)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long programId = objectMapper.readTree(createProgramResponse).get("id").asLong();

        String initialListsResponse = mockMvc.perform(get("/api/v1/admin/courses/programs/{programId}/assign", programId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode initialLists = objectMapper.readTree(initialListsResponse);

        assertThat(containsUserId(initialLists.get("in"), studentA)).isFalse();
        assertThat(containsUserId(initialLists.get("in"), studentB)).isFalse();
        assertThat(containsUserId(initialLists.get("notIn"), studentA)).isTrue();
        assertThat(containsUserId(initialLists.get("notIn"), studentB)).isTrue();

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d],
                                  "idsNotIn": []
                                }
                                """.formatted(studentA)))
                .andExpect(status().isOk());

        String afterAssignListsResponse = mockMvc.perform(get("/api/v1/admin/courses/programs/{programId}/assign", programId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode afterAssignLists = objectMapper.readTree(afterAssignListsResponse);

        assertThat(containsUserId(afterAssignLists.get("in"), studentA)).isTrue();
        assertThat(containsUserId(afterAssignLists.get("notIn"), studentA)).isFalse();
        assertThat(containsUserId(afterAssignLists.get("notIn"), studentB)).isTrue();

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [],
                                  "idsNotIn": [%d]
                                }
                                """.formatted(studentA)))
                .andExpect(status().isOk());

        String afterUnassignListsResponse = mockMvc.perform(get("/api/v1/admin/courses/programs/{programId}/assign", programId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode afterUnassignLists = objectMapper.readTree(afterUnassignListsResponse);

        assertThat(containsUserId(afterUnassignLists.get("in"), studentA)).isFalse();
        assertThat(containsUserId(afterUnassignLists.get("notIn"), studentA)).isTrue();
    }

    @Test
    void program_course_two_lists_flow_should_work() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long courseA = createCourse(adminToken, "Program Course Lists A");
        Long courseB = createCourse(adminToken, "Program Course Lists B");
        Long courseC = createCourse(adminToken, "Program Course Lists C");

        String createProgramResponse = mockMvc.perform(post("/api/v1/admin/courses/programs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program Course Lists",
                                  "description": "Two-lists contract for courses",
                                  "accessCondition": "ALL_OPEN",
                                  "blockAfterDeadline": false,
                                  "courses": [%d]
                                }
                                """.formatted(courseA)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long programId = objectMapper.readTree(createProgramResponse).get("id").asLong();

        String initialListsResponse = mockMvc.perform(get("/api/v1/admin/courses/programs/{programId}/courses/assign", programId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode initialLists = objectMapper.readTree(initialListsResponse);

        assertThat(containsCourseId(initialLists.get("in"), courseA)).isTrue();
        assertThat(containsCourseId(initialLists.get("in"), courseB)).isFalse();
        assertThat(containsCourseId(initialLists.get("notIn"), courseB)).isTrue();
        assertThat(containsCourseId(initialLists.get("notIn"), courseC)).isTrue();

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/courses/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d, %d],
                                  "idsNotIn": [%d]
                                }
                                """.formatted(courseA, courseB, courseC)))
                .andExpect(status().isOk());

        String afterAddCourseResponse = mockMvc.perform(get("/api/v1/admin/courses/programs/{programId}", programId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode afterAddCourseProgram = objectMapper.readTree(afterAddCourseResponse);

        assertThat(afterAddCourseProgram.get("courses").size()).isEqualTo(2);
        assertThat(afterAddCourseProgram.get("courses").get(0).get("courseId").asLong()).isEqualTo(courseA);
        assertThat(afterAddCourseProgram.get("courses").get(1).get("courseId").asLong()).isEqualTo(courseB);

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/courses/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d],
                                  "idsNotIn": [%d, %d]
                                }
                                """.formatted(courseB, courseA, courseC)))
                .andExpect(status().isOk());

        String afterRemoveCourseResponse = mockMvc.perform(get("/api/v1/admin/courses/programs/{programId}/courses/assign", programId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode afterRemoveCourseLists = objectMapper.readTree(afterRemoveCourseResponse);

        assertThat(containsCourseId(afterRemoveCourseLists.get("in"), courseA)).isFalse();
        assertThat(containsCourseId(afterRemoveCourseLists.get("in"), courseB)).isTrue();
        assertThat(containsCourseId(afterRemoveCourseLists.get("notIn"), courseA)).isTrue();
        assertThat(containsCourseId(afterRemoveCourseLists.get("notIn"), courseC)).isTrue();
    }

    @Test
    void should_propagate_course_enrollment_when_course_added_to_existing_program() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long courseA = createCourse(adminToken, "Program Propagation Course A");
        Long courseB = createCourse(adminToken, "Program Propagation Course B");
        Long studentId = createUser(adminToken, "program.propagation.student@example.com", "STUDENT");

        String createProgramResponse = mockMvc.perform(post("/api/v1/admin/courses/programs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program Propagation",
                                  "description": "Enrollment propagation after course list change",
                                  "accessCondition": "ALL_OPEN",
                                  "blockAfterDeadline": false,
                                  "courses": [%d]
                                }
                                """.formatted(courseA)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long programId = objectMapper.readTree(createProgramResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d],
                                  "idsNotIn": []
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseA)).isTrue();
        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseB)).isFalse();

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/courses/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [%d, %d],
                                  "idsNotIn": []
                                }
                                """.formatted(courseA, courseB)))
                .andExpect(status().isOk());

        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseB)).isTrue();
    }

    @Test
    void should_unassign_program_even_if_user_still_in_assigned_group() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long courseA = createCourse(adminToken, "Program Sticky Enrollment Course A");
        Long studentId = createUser(adminToken, "program.sticky@example.com", "STUDENT");

        String createProgramResponse = mockMvc.perform(post("/api/v1/admin/courses/programs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Program Source Agnostic Unassign",
                                  "description": "Unassign regardless of previous assignment path",
                                  "accessCondition": "ALL_OPEN",
                                  "blockAfterDeadline": false,
                                  "courses": [%d]
                                }
                                """.formatted(courseA)))
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
                                  "title": "Program Sticky Group",
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
                                  "userIds": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/groups/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": ["%s"],
                                  "idsNotIn": []
                                }
                                """.formatted(groupId)))
                .andExpect(status().isOk());

        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentId, programId)).isTrue();
        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseA)).isTrue();

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [],
                                  "idsNotIn": [%d]
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentId, programId)).isFalse();
        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseA)).isFalse();

        mockMvc.perform(post("/api/v1/admin/courses/programs/{programId}/groups/assign", programId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idsIn": [],
                                  "idsNotIn": ["%s"]
                                }
                                """.formatted(groupId)))
                .andExpect(status().isOk());

        assertThat(programEnrollmentRepository.existsByUserIdAndProgramId(studentId, programId)).isFalse();
        assertThat(enrollmentRepository.existsByUserIdAndCourseId(studentId, courseA)).isFalse();
    }

    private boolean containsUserId(JsonNode usersArray, Long userId) {
        if (usersArray == null || !usersArray.isArray()) {
            return false;
        }
        for (JsonNode userNode : usersArray) {
            if (userNode.has("id") && userNode.get("id").asLong() == userId) {
                return true;
            }
        }
        return false;
    }

    private boolean containsCourseId(JsonNode coursesArray, Long courseId) {
        if (coursesArray == null || !coursesArray.isArray()) {
            return false;
        }
        for (JsonNode courseNode : coursesArray) {
            if (courseNode.has("id") && courseNode.get("id").asLong() == courseId) {
                return true;
            }
        }
        return false;
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
