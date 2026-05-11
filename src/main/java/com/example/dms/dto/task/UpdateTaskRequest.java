package com.example.dms.dto.task;

import com.example.dms.entity.TaskPriority;
import com.example.dms.entity.TaskStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateTaskRequest {

    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private TaskPriority priority;

    private TaskStatus status;

    private LocalDate deadline;

    private Long assigneeId;
}