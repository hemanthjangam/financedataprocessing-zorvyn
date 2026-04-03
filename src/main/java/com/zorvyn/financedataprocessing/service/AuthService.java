package com.zorvyn.financedataprocessing.service;

import com.zorvyn.financedataprocessing.domain.UserAccount;
import com.zorvyn.financedataprocessing.domain.UserStatus;
import com.zorvyn.financedataprocessing.dto.AuthLoginRequest;
import com.zorvyn.financedataprocessing.dto.AuthResponse;
import com.zorvyn.financedataprocessing.exception.UnauthorizedException;
import com.zorvyn.financedataprocessing.repository.UserAccountRepository;
import com.zorvyn.financedataprocessing.security.PasetoTokenService;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasetoTokenService pasetoTokenService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            PasetoTokenService pasetoTokenService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.pasetoTokenService = pasetoTokenService;
    }

    public AuthResponse login(AuthLoginRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Return the same error for inactive users and bad passwords to avoid leaking account state.
        if (user.getStatus() == UserStatus.INACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        PasetoTokenService.IssuedToken issuedToken = pasetoTokenService.issueToken(user);

        return new AuthResponse(issuedToken.value(), "Bearer", issuedToken.expiresAt(), user.getEmail(), user.getRole());
    }

    public void logout(UserAccount user) {
        // Any token issued before this timestamp is treated as logged out.
        user.setTokenInvalidBefore(Instant.now());
    }
}
