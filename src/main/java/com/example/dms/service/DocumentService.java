package com.example.dms.service;

import com.example.dms.annotation.Audited;
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
import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DocumentService {

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

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidDocumentException("File must have a valid name.");
        }
        if (documentRepository.existsByProjectIdAndName(projectId, fileName)) {
            throw new ResourceAlreadyExistsException(
                    "A document named '" + fileName + "' already exists in this project");
        }

        String minioKey = UUID.randomUUID() + "_" + fileName;

        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(minioKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Failed to upload file to MinIO: " + e.getMessage());
        }

        DocumentEntity document = new DocumentEntity();
        document.setName(fileName);
        document.setType(file.getContentType());
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
    public InputStream downloadDocument(Long projectId, Long documentId,
                                        Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = findActiveProject(projectId);
        checkMemberOrOwner(project, user);

        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(document.getMinioKey())
                    .build());
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

        DocumentEntity document = documentRepository.findById(documentId)
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

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new FileStorageException("Failed to initialize storage bucket: " + e.getMessage());
        }
    }

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