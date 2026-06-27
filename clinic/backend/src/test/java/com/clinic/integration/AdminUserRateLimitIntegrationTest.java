package com.clinic.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
                "spring.datasource.url=jdbc:h2:mem:rate_limit_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never",
                "spring.cache.type=simple",
                "management.health.redis.enabled=false",
                "app.rate-limit.enabled=true",
                "app.rate-limit.requests=3",
                "app.rate-limit.auth-requests=3",
                "app.rate-limit.window=1m"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminUserRateLimitIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        private static final String VALID_LOGIN_BODY = """
                        {
                          "email": "nonexistent@clinic.com",
                          "password": "Password123!"
                        }
                        """;

        @Test
        void rateLimitingFilterBlocksRequestsWhenExceeded() throws Exception {
                // First 3 requests: rate limiter allows them through (auth may return 4xx —
                // that's fine)
                for (int i = 0; i < 3; i++) {
                        mockMvc.perform(post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(VALID_LOGIN_BODY))
                                        .andExpect(result -> {
                                                int status = result.getResponse().getStatus();
                                                if (status < 200 || (status >= 300 && status < 400) || status >= 500) {
                                                        throw new AssertionError(
                                                                        "Expected 2xx or 4xx but got: " + status);
                                                }
                                        });
                }

                // 4th request: rate limiter kicks in → must return 429 Too Many Requests
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_LOGIN_BODY))
                                .andExpect(status().isTooManyRequests());
        }
}