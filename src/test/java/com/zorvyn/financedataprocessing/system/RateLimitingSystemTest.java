package com.zorvyn.financedataprocessing.system;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:rate-limit-db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seed.enabled=true",
        "app.rate-limit.max-requests=2",
        "app.rate-limit.window-seconds=60"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RateLimitingSystemTest {

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
    void rateLimiterBlocksRequestsAfterConfiguredThreshold() throws Exception {
        RequestPostProcessor clientIp = request -> {
            request.setRemoteAddr("10.10.10.10");
            return request;
        };

        mockMvc.perform(get("/api/health").with(clientIp))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/summary").with(clientIp))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/dashboard/summary").with(clientIp))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/dashboard/summary").with(clientIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Rate limit exceeded. Please retry later."));
    }
}
