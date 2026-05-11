package com.example.dms.controller;

import com.example.dms.dto.task.CreateTaskRequest;
import com.example.dms.dto.task.TaskResponse;
import com.example.dms.dto.task.UpdateTaskRequest;
import com.example.dms.entity.TaskPriority;
import com.example.dms.entity.TaskStatus;
import com.example.dms.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(projectId, request, authentication));
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasksForProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            Authentication authentication) {
        return ResponseEntity.ok(taskService.getTasksForProject(projectId, status, priority, authentication));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(taskService.updateTask(projectId, taskId, request, authentication));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            Authentication authentication) {
        taskService.deleteTask(projectId, taskId, authentication);
        return ResponseEntity.noContent().build();
    }
}