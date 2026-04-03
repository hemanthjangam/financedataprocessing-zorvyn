package com.zorvyn.financedataprocessing.dto;

import com.zorvyn.financedataprocessing.domain.Role;
import com.zorvyn.financedataprocessing.domain.UserStatus;
import java.time.Instant;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        UserStatus status,
        Instant createdAt
) {
}
