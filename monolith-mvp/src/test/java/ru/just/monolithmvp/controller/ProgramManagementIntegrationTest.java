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