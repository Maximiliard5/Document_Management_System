package com.example.dms.service;

import com.example.dms.dto.auth.AuthResponse;
import com.example.dms.dto.auth.LoginRequest;
import com.example.dms.dto.auth.RegisterRequest;
import com.example.dms.entity.Role;
import com.example.dms.entity.UserEntity;
import com.example.dms.exception.EmailAlreadyExistsException;
import com.example.dms.repository.UserRepository;
import com.example.dms.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration and login. Produces JWT tokens on successful authentication
 * and audits both successful and failed login attempts.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    /**
     * Registers a new user account and returns a JWT token.
     *
     * @param request the registration details (email, password, first and last name)
     * @return an {@link AuthResponse} containing the JWT and basic user info
     * @throws EmailAlreadyExistsException if the email is already registered
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }
        var user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(Role.USER);

        userRepository.save(user);
        var token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Authenticates a user and returns a JWT token. Both successful and failed
     * attempts are recorded in the audit log. Login failure throws the original
     * Spring Security exception so the global handler can return the correct HTTP status.
     *
     * @param request the login credentials (email and password)
     * @return an {@link AuthResponse} containing the JWT and basic user info
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are wrong
     * @throws org.springframework.security.authentication.DisabledException if the account is deactivated
     */
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            auditService.log(request.getEmail(), "LOGIN_FAILURE", "USER", null, null,
                    e.getClass().getSimpleName());
            throw e;
        }

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + request.getEmail()));
        var token = jwtService.generateToken(user.getEmail(), user.getRole().name());

        auditService.log(user.getEmail(), "LOGIN_SUCCESS", "USER", user.getId().toString(), null, null);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }
}
