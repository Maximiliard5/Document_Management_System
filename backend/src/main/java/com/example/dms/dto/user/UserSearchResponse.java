package com.example.dms.dto.user;

import com.example.dms.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

/**
 * Minimal user projection returned by the {@code /users/search} endpoint. Deliberately
 * narrower than {@link UserResponse} — role and active status aren't needed by callers
 * looking up someone to invite to a project, so we don't hand that out to non-admins.
 */
@Getter
@Builder
public class UserSearchResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;

    public static UserSearchResponse toResponse(UserEntity user) {
        return UserSearchResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
}
