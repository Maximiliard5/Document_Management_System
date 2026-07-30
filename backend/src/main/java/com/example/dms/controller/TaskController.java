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

/**
 * Endpoints for task management within a project. All operations require
 * the caller to be a member or owner of the specified project.
 */
@RestController
@RequestMapping("/projects/{projectId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** POST /projects/{projectId}/tasks — creates a task in the project. Returns 201. */
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(projectId, request, authentication));
    }

    /** GET /projects/{projectId}/tasks — lists tasks with optional ?status= and ?priority= filters. */
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasksForProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            Authentication authentication) {
        return ResponseEntity.ok(taskService.getTasksForProject(projectId, status, priority, authentication));
    }

    /** PUT /projects/{projectId}/tasks/{taskId} — updates a task. Null fields are left unchanged. */
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(taskService.updateTask(projectId, taskId, request, authentication));
    }

    /** DELETE /projects/{projectId}/tasks/{taskId} — soft-deletes a task. Returns 204. */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            Authentication authentication) {
        taskService.deleteTask(projectId, taskId, authentication);
        return ResponseEntity.noContent().build();
    }
}