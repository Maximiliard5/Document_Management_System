package com.example.dms.project.dto;

import com.example.dms.project.ProjectStatus;
import com.example.dms.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@AllArgsConstructor
@Getter
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private ProjectStatus status;
    private UserResponse owner;
    private Set<UserResponse> members;
    private LocalDateTime createdAt;
}
