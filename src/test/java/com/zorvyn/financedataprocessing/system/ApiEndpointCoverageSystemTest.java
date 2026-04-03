package com.zorvyn.financedataprocessing.system;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:coverage-db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seed.enabled=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiEndpointCoverageSystemTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void loginRejectsInvalidCredentialsAndMalformedRequests() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@zorvyn.local",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details", hasSize(2)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    @Test
    void logoutRevokesExistingBearerToken() throws Exception {
        String adminToken = login("admin@zorvyn.local", "Admin@123");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    @Test
    void viewerCannotManageUsersOrRecords() throws Exception {
        String viewerToken = login("viewer@zorvyn.local", "Viewer@123");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 12.34,
                                  "type": "EXPENSE",
                                  "category": "Travel",
                                  "transactionDate": "2026-04-02"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void adminCanCreateListAndUpdateUsersWhileDuplicateEmailIsRejected() throws Exception {
        String adminToken = login("admin@zorvyn.local", "Admin@123");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Priya Ops",
                                  "email": "priya@zorvyn.local",
                                  "password": "Password@123",
                                  "role": "VIEWER",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("priya@zorvyn.local"));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Duplicate Priya",
                                  "email": "priya@zorvyn.local",
                                  "password": "Password@123",
                                  "role": "VIEWER",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this email already exists"));

        mockMvc.perform(put("/api/users/4")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Priya Operations",
                                  "email": "priya.ops@zorvyn.local",
                                  "password": "Password@123",
                                  "role": "ANALYST",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("priya.ops@zorvyn.local"))
                .andExpect(jsonPath("$.role").value("ANALYST"));
    }

    @Test
    void userEndpointsValidatePayloadsAndMissingResources() throws Exception {
        String adminToken = login("admin@zorvyn.local", "Admin@123");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "",
                                  "email": "bad-email",
                                  "password": "short",
                                  "role": null,
                                  "status": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        mockMvc.perform(put("/api/users/999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Ghost User",
                                  "email": "ghost@zorvyn.local",
                                  "password": "Password@123",
                                  "role": "VIEWER",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void analystCanFilterRecordsButInvalidPaginationAndTypesAreRejected() throws Exception {
        String analystToken = login("analyst@zorvyn.local", "Analyst@123");

        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + analystToken)
                        .queryParam("category", "Rent")
                        .queryParam("from", "2026-03-01")
                        .queryParam("to", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].category").value("Rent"));

        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + analystToken)
                        .queryParam("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page must be zero or greater"));

        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + analystToken)
                        .queryParam("type", "BROKEN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request parameter type mismatch"));
    }

    @Test
    void adminCanCreateUpdateAndSoftDeleteRecords() throws Exception {
        String adminToken = login("admin@zorvyn.local", "Admin@123");

        MvcResult createResult = mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 321.45,
                                  "type": "EXPENSE",
                                  "category": "Training",
                                  "transactionDate": "2026-04-01",
                                  "notes": "Workshop access"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("Training"))
                .andReturn();

        String recordId = extractJsonValue(createResult.getResponse().getContentAsString(), "id");

        mockMvc.perform(put("/api/records/{recordId}", recordId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 400.00,
                                  "type": "EXPENSE",
                                  "category": "Training",
                                  "transactionDate": "2026-04-01",
                                  "notes": "Workshop and materials"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(400.00));

        mockMvc.perform(delete("/api/records/{recordId}", recordId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .queryParam("category", "Training"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void recordEndpointsValidatePayloadsAndMissingResources() throws Exception {
        String adminToken = login("admin@zorvyn.local", "Admin@123");

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 0,
                                  "type": null,
                                  "category": "",
                                  "transactionDate": null,
                                  "notes": "x"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        mockMvc.perform(put("/api/records/999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 12.00,
                                  "type": "EXPENSE",
                                  "category": "Travel",
                                  "transactionDate": "2026-04-02",
                                  "notes": "Missing record"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Financial record not found"));

        mockMvc.perform(delete("/api/records/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Financial record not found"));
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        Matcher matcher = TOKEN_PATTERN.matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new AssertionError("Access token not found in login response");
        }
        return matcher.group(1);
    }

    private String extractJsonValue(String responseBody, String fieldName) {
        Pattern pattern = Pattern.compile("\"%s\"\\s*:\\s*\"?([^\",}]+)".formatted(fieldName));
        Matcher matcher = pattern.matcher(responseBody);
        if (!matcher.find()) {
            throw new AssertionError(fieldName + " not found in response");
        }
        return matcher.group(1);
    }
}
