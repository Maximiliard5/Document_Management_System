package com.example.dms.controller;

import com.example.dms.dto.user.UpdateProfileRequest;
import com.example.dms.dto.user.UserResponse;
import com.example.dms.dto.user.UserSearchResponse;
import com.example.dms.entity.Role;
import com.example.dms.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints for user profile management and admin-level account operations.
 * Admin-only endpoints are protected with {@code @PreAuthorize("hasRole('ADMIN')")}.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** GET /users/me — returns the authenticated user's own profile. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        return ResponseEntity.ok(userService.getMe(authentication));
    }

    /** PUT /users/me — updates the authenticated user's first and/or last name. */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateMe(authentication, request));
    }

    /**
     * GET /users/search?email={query} — looks up active users by partial email match.
     * Any authenticated user (not admin-only) — used to find someone to invite to a
     * project. Returns a narrower projection than {@link UserResponse}; see
     * {@link UserSearchResponse}.
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(@RequestParam String email) {
        return ResponseEntity.ok(userService.searchUsers(email));
    }

    /** GET /users — returns all users. Admin only. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /** PUT /users/{id}/role?role={ADMIN|USER} — changes a user's role. Admin only. */
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable Long id,
            @RequestParam Role role) {
        return ResponseEntity.ok(userService.updateRole(id, role));
    }

    /** PUT /users/{id}/deactivate — deactivates a user account. Admin only. */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }
}
