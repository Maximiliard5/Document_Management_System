package com.example.dms.controller;

import com.example.dms.dto.auth.AuthResponse;
import com.example.dms.dto.auth.LoginRequest;
import com.example.dms.dto.auth.RegisterRequest;
import com.example.dms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints for user registration and login. No authentication token required.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /auth/register — creates a new user account.
     *
     * @param request registration details (email, password, first and last name)
     * @return 201 Created with a JWT and user info on success
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }
    
    /**
     * POST /auth/login — authenticates credentials and returns a JWT.
     *
     * @param request login credentials (email and password)
     * @return 200 OK with a JWT and user info on success
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}