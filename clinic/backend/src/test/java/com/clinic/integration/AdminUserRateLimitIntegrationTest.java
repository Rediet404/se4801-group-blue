package com.clinic.integration;

import com.clinic.config.RateLimitingFilter;
import com.clinic.dto.request.CreateUserRequest;
import com.clinic.dto.request.UpdateUserRequest;
import com.clinic.entity.User;
import com.clinic.entity.UserRole;
import com.clinic.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:clinice_admin_rate_limit_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class AdminUserRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        rateLimitingFilter.resetLimit("127.0.0.1");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminUserCrudFlowWorks() throws Exception {
        // 1. Create a user via AdminController POST
        CreateUserRequest createRequest = new CreateUserRequest(
                "staff.member@example.com",
                "Password123!",
                "Staff Member",
                "+15552222",
                UserRole.PHARMACIST
        );

        String createResponseJson = mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertTrue(createResponseJson.contains("staff.member@example.com"));
        assertTrue(createResponseJson.contains("PHARMACIST"));

        // Fetch user from DB to get ID
        User user = userRepository.findByEmail("staff.member@example.com")
                .orElseThrow(() -> new AssertionError("User should exist"));
        String userId = user.getId();

        // 2. Update the user via AdminController PUT
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                "updated.staff@example.com",
                "Updated Staff Member",
                "+15559999",
                UserRole.LABORATORY,
                true
        );

        String updateResponseJson = mockMvc.perform(put("/api/v1/admin/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(updateResponseJson.contains("updated.staff@example.com"));
        assertTrue(updateResponseJson.contains("Updated Staff Member"));
        assertTrue(updateResponseJson.contains("LABORATORY"));

        // Verify database updated
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertEquals("updated.staff@example.com", updatedUser.getEmail());
        assertEquals("Updated Staff Member", updatedUser.getFullName());
        assertEquals(UserRole.LABORATORY, updatedUser.getRole());

        // 3. Delete the user via AdminController DELETE
        mockMvc.perform(delete("/api/v1/admin/users/" + userId))
                .andExpect(status().isNoContent());

        // Verify soft-deleted user is not visible through regular JPA query due to @SQLRestriction
        assertFalse(userRepository.findById(userId).isPresent());
    }

    @Test
    void rateLimitingFilterBlocksRequestsWhenExceeded() throws Exception {
        // Execute 100 requests successfully
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }

        // The 101st request should be blocked with 429 Too Many Requests
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isTooManyRequests());

        // Reset rate limits for test IP
        rateLimitingFilter.resetLimit("127.0.0.1");

        // Request should now succeed again
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
