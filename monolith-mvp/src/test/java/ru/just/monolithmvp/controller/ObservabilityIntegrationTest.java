package ru.just.monolithmvp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:observability-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.security.jwt.secret=test-jwt-secret-key-at-least-32-bytes-12345",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.profiles.active=h2"
})
class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void should_return_correlation_id_and_metrics_endpoint_available() throws Exception {
        String token = login("admin@local", "admin123");

        String correlationId = mockMvc.perform(get("/api/v1/student/my/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("X-Correlation-Id");

        assertThat(correlationId).isNotBlank();

        String metricsResponse = mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(metricsResponse).contains("courses_http_requests_total");
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
