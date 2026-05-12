package com.example.dms.service;

import com.example.dms.annotation.Audited;
import lombok.extern.slf4j.Slf4j;
import com.example.dms.dto.user.UpdateProfileRequest;
import com.example.dms.dto.user.UserResponse;
import com.example.dms.entity.Role;
import com.example.dms.entity.UserEntity;
import com.example.dms.exception.InvalidOperationException;
import com.example.dms.exception.ResourceNotFoundException;
import com.example.dms.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getMe(Authentication authentication) {
        var user = getUserFromAuthentication(authentication);
        return toResponse(user);
    }

    public UserResponse updateMe(Authentication authentication, UpdateProfileRequest request) {
        var user = getUserFromAuthentication(authentication);
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        userRepository.save(user);
        return toResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

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
        log.info("User role changed: userId={} newRole={}", id, role);
        return toResponse(user);
    }

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
        log.info("User deactivated: userId={} email={}", id, user.getEmail());
        return toResponse(user);
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
