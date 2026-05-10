package com.example.dms.user;

import com.example.dms.security.JwtService;
import com.example.dms.user.dto.AuthResponse;
import com.example.dms.user.dto.LoginRequest;
import com.example.dms.user.dto.RegisterRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }//if the email is already taken, throw exception
        //We build a new User object, hash the password with BCrypt, assign the default role of USER, and save it to the database. New registrations are always USER — admins are created separately.
        var user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(Role.USER);

        userRepository.save(user);
        //After saving, we immediately generate a token and return it. This means after registering, the user is automatically logged in — they don't need to call /auth/login separately.
        var token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate( //it looks up the user by email, hashes the provided password, and compares it to the stored hash. If either the email doesn't exist or the password is wrong, it throws an AuthenticationException automatically. You don't need to write that logic yourself.
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail()) //After authentication passes we know the user exists, so we load them to get their role for the token. .orElseThrow() here is safe — authenticationManager already confirmed the email exists.
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + request.getEmail()));
        //Generate and return the token, same as register.
        var token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }
}