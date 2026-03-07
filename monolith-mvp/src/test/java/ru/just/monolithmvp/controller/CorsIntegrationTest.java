package ru.just.monolithmvp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cors-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.profiles.active=h2",
        "app.security.jwt.secret=test-jwt-secret-key-at-least-32-bytes-12345",
        "app.security.cors.allowed-origins=http://localhost:3000",
        "app.storage.csp-frame-ancestors='self',http://localhost:3000,http://127.0.0.1:3000"
})
class CorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflight_for_login_should_allow_configured_origin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void preflight_for_login_should_reject_not_allowed_origin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isForbidden());
    }

    @Test
    void files_response_should_include_configured_csp_frame_ancestors_header() throws Exception {
        mockMvc.perform(get("/files/non-existing-test-file.txt"))
                .andExpect(header().string("Content-Security-Policy",
                        "frame-ancestors 'self' http://localhost:3000 http://127.0.0.1:3000"))
                .andExpect(header().doesNotExist("X-Frame-Options"));
    }

    @Test
    void non_files_response_should_include_sameorigin_x_frame_options_header() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }
}
