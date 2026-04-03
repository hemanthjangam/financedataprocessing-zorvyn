package com.zorvyn.financedataprocessing.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.zorvyn.financedataprocessing.domain.Role;
import com.zorvyn.financedataprocessing.domain.UserAccount;
import com.zorvyn.financedataprocessing.domain.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PasetoTokenServiceTest {

    @Test
    void issuedTokenCanBeParsedBackIntoClaims() {
        PasetoTokenService pasetoTokenService = new PasetoTokenService(
                "finance-data-processing",
                "finance-dashboard",
                12
        );

        UserAccount user = UserAccount.builder()
                .id(42L)
                .fullName("Test Admin")
                .email("admin@example.com")
                .passwordHash("ignored")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        PasetoTokenService.IssuedToken issuedToken = pasetoTokenService.issueToken(user);
        PasetoTokenService.ParsedToken parsedToken = pasetoTokenService.parseAndValidate(issuedToken.value());

        assertNotNull(issuedToken.value());
        assertEquals(user.getId(), parsedToken.userId());
        assertEquals(user.getEmail(), parsedToken.email());
        assertEquals(user.getRole(), parsedToken.role());
    }
}
