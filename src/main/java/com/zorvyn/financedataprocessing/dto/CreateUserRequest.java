package com.zorvyn.financedataprocessing.dto;

import com.zorvyn.financedataprocessing.domain.Role;
import com.zorvyn.financedataprocessing.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 120) String password,
        @NotNull Role role,
        @NotNull UserStatus status
) {
}
