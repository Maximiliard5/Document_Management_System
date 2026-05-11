package com.example.dms.dto;

import com.example.dms.entity.ProjectStatus;
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
