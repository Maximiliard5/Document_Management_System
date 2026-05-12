package com.example.dms.service;

import com.example.dms.annotation.Audited;
import com.example.dms.dto.document.DocumentDownload;
import com.example.dms.dto.document.DocumentResponse;
import com.example.dms.entity.DocumentEntity;
import com.example.dms.entity.ProjectEntity;
import com.example.dms.entity.UserEntity;
import com.example.dms.exception.FileStorageException;
import com.example.dms.exception.InvalidDocumentException;
import com.example.dms.exception.ResourceAlreadyExistsException;
import com.example.dms.exception.ResourceNotFoundException;
import com.example.dms.repository.DocumentRepository;
import com.example.dms.repository.ProjectRepository;
import com.example.dms.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50 MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "text/plain",
            "text/csv",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/msword",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint"
    );
    private static final Tika TIKA = new Tika();

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public DocumentService(DocumentRepository documentRepository,
                           ProjectRepository projectRepository,
                           UserRepository userRepository,
                           MinioClient minioClient) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.minioClient = minioClient;
    }

    @Audited(action = "DOCUMENT_UPLOAD", entityType = "DOCUMENT",
            entityIdExpression = "#result.id.toString()",
            projectIdExpression = "#projectId",
            detailsExpression = "#file.originalFilename")
    @Transactional
    public DocumentResponse uploadDocument(Long projectId, MultipartFile file,
                                           Authentication authentication) {
        UserEntity owner = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(projectId);
        checkMemberOrOwner(project, owner);

        if (file.isEmpty()) {
            throw new InvalidDocumentException("File must not be empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidDocumentException("File exceeds the maximum allowed size of 50 MB.");
        }

        String detectedMime;
        try (InputStream headerStream = file.getInputStream()) {
            detectedMime = TIKA.detect(headerStream);
        } catch (IOException e) {
            throw new InvalidDocumentException("Could not read file content.");
        }
        if (!ALLOWED_MIME_TYPES.contains(detectedMime)) {
            throw new InvalidDocumentException("File type '" + detectedMime + "' is not allowed.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidDocumentException("File must have a valid name.");
        }
        if (documentRepository.existsByProjectIdAndName(projectId, fileName)) {
            throw new ResourceAlreadyExistsException(
                    "A document named '" + fileName + "' already exists in this project");
        }

        String minioKey = UUID.randomUUID().toString();

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(minioKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(detectedMime)
                    .build());
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Failed to upload file to MinIO: " + e.getMessage());
        }

        DocumentEntity document = new DocumentEntity();
        document.setName(fileName);
        document.setType(detectedMime);
        document.setSize(file.getSize());
        document.setMinioKey(minioKey);
        document.setProject(project);
        document.setOwner(owner);

        DocumentResponse response = toResponse(documentRepository.save(document));
        log.info("Document uploaded: id={} name='{}' projectId={} owner={}", response.getId(), response.getName(), projectId, owner.getEmail());
        return response;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(Long projectId, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(projectId);
        checkMemberOrOwner(project, user);

        return documentRepository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Audited(action = "DOCUMENT_DOWNLOAD", entityType = "DOCUMENT",
            entityIdExpression = "#documentId.toString()",
            projectIdExpression = "#projectId")
    @Transactional(readOnly = true)
    public DocumentDownload downloadDocument(Long projectId, Long documentId,
                                             Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(projectId);
        checkMemberOrOwner(project, user);

        DocumentEntity document = documentRepository.findByIdAndProjectId(documentId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(document.getMinioKey())
                    .build());
            return new DocumentDownload(stream, document.getName(), document.getType(), document.getSize());
        } catch (Exception e) {
            throw new FileStorageException("Failed to download file from MinIO: " + e.getMessage());
        }
    }

    @Audited(action = "DOCUMENT_DELETE", entityType = "DOCUMENT",
            entityIdExpression = "#documentId.toString()",
            projectIdExpression = "#projectId")
    @Transactional
    public void deleteDocument(Long projectId, Long documentId, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(projectId);
        checkMemberOrOwner(project, user);

        DocumentEntity document = documentRepository.findByIdAndProjectId(documentId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(document.getMinioKey())
                    .build());
        } catch (Exception e) {
            throw new FileStorageException("Failed to delete file from MinIO: " + e.getMessage());
        }

        documentRepository.delete(document);
        log.info("Document deleted: id={} projectId={} by={}", documentId, projectId, user.getEmail());
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

    private void checkMemberOrOwner(ProjectEntity project, UserEntity user) {
        boolean isOwner = project.getOwner().getId().equals(user.getId());
        boolean isMember = project.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
        if (!isOwner && !isMember) {
            throw new AccessDeniedException("You do not have access to this project");
        }
    }

    private DocumentResponse toResponse(DocumentEntity document) {
        return new DocumentResponse(
                document.getId(),
                document.getName(),
                document.getType(),
                document.getSize(),
                document.getProject().getId(),
                document.getOwner().getId(),
                document.getCreatedAt()
        );
    }
}