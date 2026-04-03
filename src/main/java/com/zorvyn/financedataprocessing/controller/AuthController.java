package com.zorvyn.financedataprocessing.controller;

import com.zorvyn.financedataprocessing.dto.AuthLoginRequest;
import com.zorvyn.financedataprocessing.dto.AuthResponse;
import com.zorvyn.financedataprocessing.security.CurrentUserService;
import com.zorvyn.financedataprocessing.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthLoginRequest request) {
        // Login returns a bearer token the client can reuse on protected endpoints.
        return authService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void logout() {
        // Logout invalidates tokens issued before this point for the current user.
        authService.logout(currentUserService.requireCurrentUser());
    }
}
