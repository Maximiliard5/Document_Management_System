package com.example.dms.service;

import com.example.dms.annotation.Audited;
import com.example.dms.dto.user.UpdateProfileRequest;
import com.example.dms.dto.user.UserResponse;
import com.example.dms.dto.user.UserSearchResponse;
import com.example.dms.entity.Role;
import com.example.dms.entity.UserEntity;
import com.example.dms.exception.InvalidOperationException;
import com.example.dms.exception.ResourceNotFoundException;
import com.example.dms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages user profiles and admin-level account operations such as role changes
 * and account deactivation.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param authentication the current security context
     * @return the caller's profile as a {@link UserResponse}
     */
    @Transactional(readOnly = true)
    public UserResponse getMe(Authentication authentication) {
        var user = getUserFromAuthentication(authentication);
        return toResponse(user);
    }

    /**
     * Updates the first and/or last name of the authenticated user.
     * Fields that are {@code null} in the request are left unchanged.
     *
     * @param authentication the current security context
     * @param request        the fields to update; null fields are skipped
     * @return the updated profile as a {@link UserResponse}
     */
    public UserResponse updateMe(Authentication authentication, UpdateProfileRequest request) {
        var user = getUserFromAuthentication(authentication);
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        userRepository.save(user);
        return toResponse(user);
    }

    /**
     * Returns all registered users. Intended for admin use only.
     *
     * @return list of all users as {@link UserResponse} objects
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Changes the role of a user. Prevents demoting the last active admin to avoid
     * locking all admin functionality out of the system.
     *
     * @param id   the ID of the user whose role should change
     * @param role the new role to assign
     * @return the updated user as a {@link UserResponse}
     * @throws ResourceNotFoundException  if no user exists with the given ID
     * @throws InvalidOperationException  if the change would leave zero active admins
     */
    @Audited(action = "USER_ROLE_CHANGED", entityType = "USER",
            entityIdExpression = "#id.toString()",
            detailsExpression = "#role.name()")
    public UserResponse updateRole(Long id, Role role) {
        var user = findUserById(id);
        if (user.getRole() == Role.ADMIN && role != Role.ADMIN) {
            if (userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
                throw new InvalidOperationException("Cannot demote the last active admin.");
            }
        }
        user.setRole(role);
        userRepository.save(user);
        return toResponse(user);
    }

    /**
     * Deactivates a user account, immediately invalidating all their active JWT tokens
     * since the filter checks {@code isEnabled()} on every request. Prevents deactivating
     * the last active admin.
     *
     * @param id the ID of the user to deactivate
     * @return the updated user as a {@link UserResponse}
     * @throws ResourceNotFoundException if no user exists with the given ID
     * @throws InvalidOperationException if the user is the last active admin
     */
    @Audited(action = "USER_DEACTIVATED", entityType = "USER",
            entityIdExpression = "#id.toString()")
    public UserResponse deactivateUser(Long id) {
        var user = findUserById(id);
        if (user.getRole() == Role.ADMIN) {
            if (userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
                throw new InvalidOperationException("Cannot deactivate the last active admin.");
            }
        }
        user.setActive(false);
        userRepository.save(user);
        return toResponse(user);
    }

    /**
     * Looks up active users by a partial, case-insensitive email match. Used by any
     * authenticated user to find someone to invite as a project member — unlike
     * {@link #getAllUsers()} this is not admin-only, so the response is deliberately
     * limited to non-sensitive fields (see {@link UserSearchResponse}) and to a small
     * number of results.
     *
     * @param query the (partial) email to search for; queries shorter than 2 characters
     *              return no results rather than pulling a large chunk of the table
     * @return up to 10 matching active users
     */
    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        return userRepository.findTop10ByEmailContainingIgnoreCaseAndActiveTrue(query.trim())
                .stream()
                .map(UserSearchResponse::toResponse)
                .toList();
    }

    private UserEntity findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private UserEntity getUserFromAuthentication(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private UserResponse toResponse(UserEntity user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }
}
