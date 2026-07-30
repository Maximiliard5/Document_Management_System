package com.example.dms.service;

import com.example.dms.annotation.Audited;
import com.example.dms.dto.project.CreateProjectRequest;
import com.example.dms.dto.project.ProjectResponse;
import com.example.dms.dto.project.UpdateProjectRequest;
import com.example.dms.dto.user.UserResponse;
import com.example.dms.entity.ProjectEntity;
import com.example.dms.entity.UserEntity;
import com.example.dms.exception.InvalidOperationException;
import com.example.dms.exception.ResourceNotFoundException;
import com.example.dms.repository.ProjectRepository;
import com.example.dms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages the full lifecycle of projects: creation, retrieval, updates,
 * member management, and soft deletion.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new project owned by the authenticated user.
     *
     * @param request        the project name and optional description
     * @param authentication the current security context
     * @return the created project as a {@link ProjectResponse}
     */
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, Authentication authentication) {
        UserEntity owner = getAuthenticatedUser(authentication);
        ProjectEntity project = new ProjectEntity();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwner(owner);
        return toResponse(projectRepository.save(project));
    }

    /**
     * Returns all active projects that the authenticated user owns or is a member of.
     *
     * @param authentication the current security context
     * @return list of visible projects as {@link ProjectResponse} objects
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getMyProjects(Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        return projectRepository.findAllByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a single project. The caller must be the owner or a member.
     *
     * @param id             the project ID
     * @param authentication the current security context
     * @return the project as a {@link ProjectResponse}
     * @throws ResourceNotFoundException if the project does not exist or is deleted
     * @throws AccessDeniedException     if the caller is not a member or owner
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long id, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(id);
        checkMemberOrOwner(project, user);
        return toResponse(project);
    }

    /**
     * Updates a project's name and/or description. Only the project owner can call this.
     * Null fields in the request are left unchanged.
     *
     * @param id             the project ID
     * @param request        the fields to update; null fields are skipped
     * @param authentication the current security context
     * @return the updated project as a {@link ProjectResponse}
     * @throws ResourceNotFoundException if the project does not exist or is deleted
     * @throws AccessDeniedException     if the caller is not the project owner
     */
    @Transactional
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(id);
        checkOwner(project, user);

        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());

        return toResponse(projectRepository.save(project));
    }

    /**
     * Adds a user as a member of a project. Only the project owner can add members.
     * The owner cannot be added again (they are implicitly a member), and duplicate
     * members are rejected.
     *
     * @param projectId      the project to add the member to
     * @param userId         the ID of the user to add
     * @param authentication the current security context
     * @return the updated project as a {@link ProjectResponse}
     * @throws ResourceNotFoundException if the project or user does not exist
     * @throws AccessDeniedException     if the caller is not the project owner
     * @throws InvalidOperationException if the user is already a member or is the owner
     */
    @Audited(action = "PROJECT_MEMBER_ADDED", entityType = "PROJECT",
            entityIdExpression = "#projectId.toString()",
            projectIdExpression = "#projectId",
            detailsExpression = "'userId=' + #userId")
    @Transactional
    public ProjectResponse addMember(Long projectId, Long userId, Authentication authentication) {
        UserEntity requester = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(projectId);
        checkOwner(project, requester);

        UserEntity newMember = findUserById(userId);
        if (project.getOwner().getId().equals(newMember.getId())) {
            throw new InvalidOperationException("Owner is already implicitly a member of the project.");
        }
        if (!project.getMembers().add(newMember)) {
            throw new InvalidOperationException("User is already a member of this project.");
        }
        return toResponse(projectRepository.save(project));
    }

    /**
     * Removes a member from a project. Only the project owner can remove members.
     *
     * @param projectId      the project to remove the member from
     * @param userId         the ID of the user to remove
     * @param authentication the current security context
     * @return the updated project as a {@link ProjectResponse}
     * @throws ResourceNotFoundException if the project does not exist or the user was not a member
     * @throws AccessDeniedException     if the caller is not the project owner
     */
    @Audited(action = "PROJECT_MEMBER_REMOVED", entityType = "PROJECT",
            entityIdExpression = "#projectId.toString()",
            projectIdExpression = "#projectId",
            detailsExpression = "'userId=' + #userId")
    @Transactional
    public ProjectResponse removeMember(Long projectId, Long userId, Authentication authentication) {
        UserEntity requester = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(projectId);
        checkOwner(project, requester);

        UserEntity member = findUserById(userId);
        if (!project.getMembers().remove(member)) {
            throw new ResourceNotFoundException("User is not a member of this project.");
        }
        return toResponse(projectRepository.save(project));
    }

    /**
     * Soft-deletes a project by setting its {@code deleted} flag to {@code true}.
     * Only the project owner can delete a project.
     *
     * @param id             the project ID
     * @param authentication the current security context
     * @throws ResourceNotFoundException if the project does not exist or is already deleted
     * @throws AccessDeniedException     if the caller is not the project owner
     */
    @Audited(action = "PROJECT_DELETE", entityType = "PROJECT",
            entityIdExpression = "#id.toString()")
    @Transactional
    public void deleteProject(Long id, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(id);
        checkOwner(project, user);
        project.setDeleted(true);
        projectRepository.save(project);
    }

    // --- helpers ---

    private UserEntity getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserEntity findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ProjectEntity findActiveProject(Long id) {
        ProjectEntity project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (project.isDeleted()) throw new ResourceNotFoundException("Project not found");
        return project;
    }

    private void checkOwner(ProjectEntity project, UserEntity user) {
        if (!project.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the project owner can perform this action");
        }
    }

    private void checkMemberOrOwner(ProjectEntity project, UserEntity user) {
        boolean isOwner = project.getOwner().getId().equals(user.getId());
        boolean isMember = project.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
        if (!isOwner && !isMember) {
            throw new AccessDeniedException("You do not have access to this project");
        }
    }

    private ProjectResponse toResponse(ProjectEntity project) {
        UserResponse ownerResponse = UserResponse.toResponse(project.getOwner());
        Set<UserResponse> memberResponses = project.getMembers()
                .stream()
                .map(UserResponse::toResponse)
                .collect(Collectors.toSet());
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                ownerResponse,
                memberResponses,
                project.getCreatedAt()
        );
    }
}
