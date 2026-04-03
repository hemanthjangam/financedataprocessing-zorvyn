package com.zorvyn.financedataprocessing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zorvyn.financedataprocessing.dto.AuthLoginRequest;
import com.zorvyn.financedataprocessing.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void loginCreatesRevocableBearerToken() {
        var response = authService.login(new AuthLoginRequest("admin@zorvyn.local", "Admin@123"));

        assertNotNull(response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals("admin@zorvyn.local", response.email());

        var user = userAccountRepository.findByEmailIgnoreCase("admin@zorvyn.local").orElseThrow();
        authService.logout(user);

        assertNotNull(user.getTokenInvalidBefore());
        assertTrue(user.getTokenInvalidBefore().isBefore(response.expiresAt()));
    }
}
