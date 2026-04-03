package com.zorvyn.financedataprocessing.service;

import com.zorvyn.financedataprocessing.domain.UserAccount;
import com.zorvyn.financedataprocessing.dto.CreateUserRequest;
import com.zorvyn.financedataprocessing.dto.UpdateUserRequest;
import com.zorvyn.financedataprocessing.dto.UserResponse;
import com.zorvyn.financedataprocessing.exception.ConflictException;
import com.zorvyn.financedataprocessing.exception.NotFoundException;
import com.zorvyn.financedataprocessing.repository.UserAccountRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        // Map entities at the service boundary so controllers stay thin.
        return userAccountRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse createUser(CreateUserRequest request) {
        if (userAccountRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A user with this email already exists");
        }

        // Normalize email before saving so uniqueness checks remain reliable.
        UserAccount user = UserAccount.builder()
                .fullName(request.fullName().trim())
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .status(request.status())
                .createdAt(Instant.now())
                .build();

        return toResponse(userAccountRepository.save(user));
    }

    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String normalizedEmail = request.email().trim().toLowerCase();
        // Reject duplicate emails before mutating the existing user.
        userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ConflictException("Another user already uses this email");
                });

        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setRole(request.role());
        user.setStatus(request.status());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return toResponse(user);
    }

    private UserResponse toResponse(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
