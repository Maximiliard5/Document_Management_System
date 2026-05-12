package com.example.dms.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice //Marks this class as a global exception handler. Spring watches every controller and when an exception is thrown, it checks here first for a matching @ExceptionHandler method.
public class GlobalExceptionHandler {
    //This fires when @Valid catches a validation failure — empty email, short password, etc. Instead of a generic error we return exactly which fields failed and why
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex){
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }
    //This catches EmailAlreadyExistsException thrown from AuthService when a registration uses a taken email.
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(EmailAlreadyExistsException ex){
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAlreadyExists(ResourceAlreadyExistsException ex){
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOperation(InvalidOperationException ex){
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidDocumentException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDocument(InvalidDocumentException ex){
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex){
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        return buildResponse(HttpStatus.BAD_REQUEST, message, null);
    }
    //This fires when AuthenticationManager rejects wrong credentials during login. Returns 401 with a safe message — notice we say "Invalid email or password" rather than specifying which one was wrong, so attackers can't use the error to enumerate valid emails.
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex){
        log.warn("Failed login attempt: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password", null);
    }
    //This fires when a user tries to log in but their account has been deactivated by an admin (active = false). Spring Security throws DisabledException automatically because UserDetailsServiceImpl passes user.isActive() to the UserDetails constructor.
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex){
        return buildResponse(HttpStatus.UNAUTHORIZED, "Account is disabled", null);
    }
    //The catch-all. Any exception we didn't specifically handle lands here. Returns 500 with a generic message — we never expose internal error details to the client in production
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Map<String, Object>> handleFileStorage(FileStorageException ex) {
        log.error("File storage error: {}", ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex){
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex){
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorizationDenied(
            org.springframework.security.authorization.AuthorizationDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied", null);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message, Object details){
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("message", message);
        if (details != null){
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }

}
