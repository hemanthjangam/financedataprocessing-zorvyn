package com.zorvyn.financedataprocessing.dto;

import com.zorvyn.financedataprocessing.domain.Role;
import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        String email,
        Role role
) {
}
