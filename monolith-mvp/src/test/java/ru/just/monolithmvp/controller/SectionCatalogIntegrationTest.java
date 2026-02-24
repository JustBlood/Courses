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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:section-catalog-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.profiles.active=h2"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SectionCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void section_crud_and_create_course_in_section_should_work() throws Exception {
        String adminToken = login("admin@local", "admin123");

        String createSectionResponse = mockMvc.perform(post("/api/v1/admin/sections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Java",
                                  "description": "Java section",
                                  "priority": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long sectionId = objectMapper.readTree(createSectionResponse).get("id").asLong();

        String createCourseInSectionResponse = mockMvc.perform(post("/api/v1/admin/sections/{sectionId}/courses", sectionId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Java Basics",
                                  "description": "Java basics course",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 70,
                                  "deadlineDays": 30
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdCourse = objectMapper.readTree(createCourseInSectionResponse);
        Long courseId = createdCourse.get("id").asLong();
        assertThat(createdCourse.get("sectionId").asLong()).isEqualTo(sectionId);
        assertThat(createdCourse.get("sectionTitle").asText()).isEqualTo("Java");
        assertThat(createdCourse.get("sectionPriority").asInt()).isEqualTo(10);

        String updateSectionResponse = mockMvc.perform(put("/api/v1/admin/sections/{sectionId}", sectionId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Java Updated",
                                  "description": "Updated description",
                                  "priority": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode updatedSection = objectMapper.readTree(updateSectionResponse);
        assertThat(updatedSection.get("title").asText()).isEqualTo("Java Updated");
        assertThat(updatedSection.get("priority").asInt()).isEqualTo(3);

        String sectionByIdResponse = mockMvc.perform(get("/api/v1/admin/sections/{sectionId}", sectionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode sectionById = objectMapper.readTree(sectionByIdResponse);
        assertThat(sectionById.get("title").asText()).isEqualTo("Java Updated");

        mockMvc.perform(delete("/api/v1/admin/sections/{sectionId}", sectionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/sections/{sectionId}", sectionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        String courseAfterSectionDeleteResponse = mockMvc.perform(get("/api/v1/admin/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode adminCourse = objectMapper.readTree(courseAfterSectionDeleteResponse).get("course");
        assertThat(adminCourse.get("sectionId").isNull()).isTrue();
        assertThat(adminCourse.get("sectionTitle").isNull()).isTrue();
        assertThat(adminCourse.get("sectionPriority").isNull()).isTrue();
    }

    @Test
    void catalog_should_be_sorted_by_section_priority_and_contain_section_fields() throws Exception {
        String adminToken = login("admin@local", "admin123");

        Long lowPrioritySectionId = createSection(adminToken, "Low", 1);
        Long highPrioritySectionId = createSection(adminToken, "High", 5);

        createCourseInSection(adminToken, lowPrioritySectionId, "Course in low section");
        createCourseInSection(adminToken, highPrioritySectionId, "Course in high section");

        mockMvc.perform(post("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Course without section",
                                  "description": "No section",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 70,
                                  "deadlineDays": 30
                                }
                                """))
                .andExpect(status().isCreated());

        String allCoursesResponse = mockMvc.perform(get("/api/v1/admin/courses")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode courses = objectMapper.readTree(allCoursesResponse);

        JsonNode lowSectionCourse = findCourseByTitle(courses, "Course in low section");
        JsonNode highSectionCourse = findCourseByTitle(courses, "Course in high section");
        JsonNode noSectionCourse = findCourseByTitle(courses, "Course without section");

        assertThat(lowSectionCourse.get("sectionId").asLong()).isEqualTo(lowPrioritySectionId);
        assertThat(lowSectionCourse.get("sectionTitle").asText()).isEqualTo("Low");
        assertThat(lowSectionCourse.get("sectionPriority").asInt()).isEqualTo(1);

        assertThat(highSectionCourse.get("sectionId").asLong()).isEqualTo(highPrioritySectionId);
        assertThat(highSectionCourse.get("sectionTitle").asText()).isEqualTo("High");
        assertThat(highSectionCourse.get("sectionPriority").asInt()).isEqualTo(5);

        assertThat(noSectionCourse.get("sectionId").isNull()).isTrue();
        assertThat(noSectionCourse.get("sectionTitle").isNull()).isTrue();
        assertThat(noSectionCourse.get("sectionPriority").isNull()).isTrue();

        int lowIndex = indexOfCourse(courses, "Course in low section");
        int highIndex = indexOfCourse(courses, "Course in high section");
        int noSectionIndex = indexOfCourse(courses, "Course without section");

        assertThat(lowIndex).isLessThan(highIndex);
        assertThat(highIndex).isLessThan(noSectionIndex);
    }

    private JsonNode findCourseByTitle(JsonNode courses, String title) {
        for (JsonNode course : courses) {
            if (title.equals(course.path("title").asText())) {
                return course;
            }
        }
        throw new AssertionError("Course not found in catalog: " + title);
    }

    private int indexOfCourse(JsonNode courses, String title) {
        for (int i = 0; i < courses.size(); i++) {
            if (title.equals(courses.get(i).path("title").asText())) {
                return i;
            }
        }
        throw new AssertionError("Course index not found in catalog: " + title);
    }

    private Long createSection(String adminToken, String title, int priority) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/sections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "%s section",
                                  "priority": %d
                                }
                                """.formatted(title, title, priority)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void createCourseInSection(String adminToken, Long sectionId, String courseTitle) throws Exception {
        mockMvc.perform(post("/api/v1/admin/sections/{sectionId}/courses", sectionId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Course description",
                                  "authorFullName": "Admin",
                                  "passingThresholdPercent": 70,
                                  "deadlineDays": 30
                                }
                                """.formatted(courseTitle)))
                .andExpect(status().isCreated());
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