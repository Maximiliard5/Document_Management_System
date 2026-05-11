package com.example.dms.service;

import com.example.dms.dto.task.CreateTaskRequest;
import com.example.dms.dto.task.TaskResponse;
import com.example.dms.dto.task.UpdateTaskRequest;
import com.example.dms.dto.user.UserResponse;
import com.example.dms.entity.*;
import com.example.dms.exception.ResourceNotFoundException;
import com.example.dms.repository.ProjectRepository;
import com.example.dms.repository.TaskRepository;
import com.example.dms.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@AllArgsConstructor
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponse createTask(Long projectId, CreateTaskRequest request, Authentication authentication) {
        UserEntity creator = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(projectId);
        checkMemberOrOwner(project, creator);

        TaskEntity task = new TaskEntity();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDeadline(request.getDeadline());
        task.setProject(project);
        task.setCreator(creator);

        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getAssigneeId() != null) {
            UserEntity assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            task.setAssignee(assignee);
        }

        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksForProject(Long projectId, TaskStatus status,
                                                 TaskPriority priority, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(projectId);
        checkMemberOrOwner(project, user);

        List<TaskEntity> tasks;

        if (status != null && priority != null) {
            tasks = taskRepository.findByProjectIdAndStatusAndPriorityAndDeletedFalse(projectId, status, priority);
        } else if (status != null) {
            tasks = taskRepository.findByProjectIdAndStatusAndDeletedFalse(projectId, status);
        } else if (priority != null) {
            tasks = taskRepository.findByProjectIdAndPriorityAndDeletedFalse(projectId, priority);
        } else {
            tasks = taskRepository.findByProjectIdAndDeletedFalse(projectId);
        }

        return tasks.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TaskResponse updateTask(Long projectId, Long taskId,
                                   UpdateTaskRequest request, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(projectId);
        checkMemberOrOwner(project, user);
        TaskEntity task = findActiveTask(taskId);

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getDeadline() != null) task.setDeadline(request.getDeadline());
        if (request.getAssigneeId() != null) {
            UserEntity assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            task.setAssignee(assignee);
        }

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long projectId, Long taskId, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(projectId);
        checkMemberOrOwner(project, user);
        TaskEntity task = findActiveTask(taskId);
        task.setDeleted(true);
        taskRepository.save(task);
    }

    // --- helpers ---

    private UserEntity getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ProjectEntity findActiveProject(Long id) {
        ProjectEntity project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (project.isDeleted()) throw new ResourceNotFoundException("Project not found");
        return project;
    }

    private TaskEntity findActiveTask(Long id) {
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (task.isDeleted()) throw new ResourceNotFoundException("Task not found");
        return task;
    }

    private void checkMemberOrOwner(ProjectEntity project, UserEntity user) {
        boolean isOwner = project.getOwner().getId().equals(user.getId());
        boolean isMember = project.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
        if (!isOwner && !isMember) {
            throw new AccessDeniedException("You do not have access to this project");
        }
    }

    private TaskResponse toResponse(TaskEntity task) {
        UserResponse assigneeResponse = task.getAssignee() != null
                ? UserResponse.toResponse(task.getAssignee())
                : null;
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .status(task.getStatus())
                .deadline(task.getDeadline())
                .creator(UserResponse.toResponse(task.getCreator()))
                .assignee(assigneeResponse)
                .projectId(task.getProject().getId())
                .createdAt(task.getCreatedAt())
                .build();
    }
}