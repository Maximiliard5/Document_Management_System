package com.example.dms.dto.task;

import com.example.dms.dto.user.UserResponse;
import com.example.dms.entity.TaskPriority;
import com.example.dms.entity.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private LocalDate deadline;
    private UserResponse creator;
    private UserResponse assignee;
    private Long projectId;
    private LocalDateTime createdAt;
}