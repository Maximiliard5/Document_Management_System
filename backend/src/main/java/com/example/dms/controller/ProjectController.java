package com.example.dms.controller;

import com.example.dms.dto.project.CreateProjectRequest;
import com.example.dms.dto.project.ProjectResponse;
import com.example.dms.dto.project.UpdateProjectRequest;
import com.example.dms.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints for project CRUD operations and member management.
 * All endpoints require authentication. Member management is restricted to the project owner.
 */
@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /** POST /projects — creates a new project owned by the caller. Returns 201. */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request, authentication));
    }

    /** GET /projects — lists all projects the caller owns or is a member of. */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getMyProjects(Authentication authentication) {
        return ResponseEntity.ok(projectService.getMyProjects(authentication));
    }

    /** GET /projects/{id} — returns a single project. Caller must be owner or member. */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(projectService.getProject(id, authentication));
    }

    /** PUT /projects/{id} — updates a project's name and/or description. Owner only. */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(projectService.updateProject(id, request, authentication));
    }

    /** POST /projects/{id}/members/{userId} — adds a user as a project member. Owner only. */
    @PostMapping("/{id}/members/{userId}")
    public ResponseEntity<ProjectResponse> addMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(projectService.addMember(id, userId, authentication));
    }

    /** DELETE /projects/{id}/members/{userId} — removes a member from a project. Owner only. */
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ProjectResponse> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(projectService.removeMember(id, userId, authentication));
    }

    /** DELETE /projects/{id} — soft-deletes a project. Owner only. Returns 204. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            Authentication authentication) {
        projectService.deleteProject(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
