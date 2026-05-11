package com.example.dms.project;

import com.example.dms.exception.ResourceNotFoundException;
import com.example.dms.project.dto.CreateProjectRequest;
import com.example.dms.project.dto.ProjectResponse;
import com.example.dms.project.dto.UpdateProjectRequest;
import com.example.dms.user.User;
import com.example.dms.user.UserRepository;
import com.example.dms.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, Authentication authentication) {
        User owner = getAuthenticatedUser(authentication);
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwner(owner);
        return toResponse(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getMyProjects(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return projectRepository.findAllByUser(user)
                .stream()
                .filter(p -> !p.isDeleted())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        Project project = findActiveProject(id);
        checkMemberOrOwner(project, user);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        Project project = findActiveProject(id);
        checkOwner(project, user);

        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());

        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse addMember(Long projectId, Long userId, Authentication authentication) {
        User requester = getAuthenticatedUser(authentication);
        Project project = findActiveProject(projectId);
        checkOwner(project, requester);

        User newMember = findUserById(userId);
        project.getMembers().add(newMember);
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse removeMember(Long projectId, Long userId, Authentication authentication) {
        User requester = getAuthenticatedUser(authentication);
        Project project = findActiveProject(projectId);
        checkOwner(project, requester);

        User member = findUserById(userId);
        project.getMembers().remove(member);
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        Project project = findActiveProject(id);
        checkOwner(project, user);
        project.setDeleted(true);
        projectRepository.save(project);
    }

    // --- helpers ---

    private User getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Project findActiveProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (project.isDeleted()) throw new ResourceNotFoundException("Project not found");
        return project;
    }

    private void checkOwner(Project project, User user) {
        if (!project.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the project owner can perform this action");
        }
    }

    private void checkMemberOrOwner(Project project, User user) {
        boolean isOwner = project.getOwner().getId().equals(user.getId());
        boolean isMember = project.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
        if (!isOwner && !isMember) {
            throw new AccessDeniedException("You do not have access to this project");
        }
    }

    private ProjectResponse toResponse(Project project) {
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