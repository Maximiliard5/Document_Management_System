package com.example.dms.user;

import com.example.dms.user.dto.AuthResponse;
import com.example.dms.user.dto.LoginRequest;
import com.example.dms.user.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController //Tells Spring this class handles HTTP requests and that every method returns data (JSON) directly in the response body. Combination of @Controller and @ResponseBody
@RequestMapping("/auth") //All endpoints in this controller are prefixed with /auth. So @PostMapping("/register") becomes POST /auth/register and @PostMapping("/login") becomes POST /auth/login
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register") //Means this method handles POST requests. Register and login are both POST because they send data in the request body.
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }
    //@RequestBody — tells Spring to take the JSON from the request body and deserialize it into a RegisterRequest object
    //@Valid — triggers the validation annotations you put on the DTO fields (@NotBlank, @Email, @Size etc.). If validation fails, Spring automatically returns a 400 before your method even runs

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }
    //201 = HttpStatus.CREATED = Created for register — a new resource (user) was created
    //200 OK for login — no new resource was created, just returning a token
}
