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
import lombok.RequiredArgsConstructor;
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

/**
 * Manages document upload, listing, download, and deletion. Document metadata
 * (name, type, size) is stored in PostgreSQL; the actual file content is stored
 * in MinIO. Documents are hard-deleted — removing a document clears both the
 * database row and the MinIO object immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
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

    /**
     * Validates and uploads a file to the project. MIME type is detected via Apache Tika
     * (not the client-supplied Content-Type). The MinIO object key is a UUID — the original
     * filename is stored only in the database as display metadata.
     *
     * @param projectId      the project to attach the document to
     * @param file           the multipart file to upload (max 50 MB, allow-listed MIME types)
     * @param authentication the current security context
     * @return metadata of the saved document as a {@link DocumentResponse}
     * @throws ResourceNotFoundException     if the project does not exist or is deleted
     * @throws AccessDeniedException         if the caller is not a member or owner
     * @throws InvalidDocumentException      if the file is empty, too large, has a disallowed type, or has no name
     * @throws ResourceAlreadyExistsException if a document with the same name already exists in the project
     * @throws FileStorageException          if the MinIO upload fails
     */
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

    /**
     * Returns all document metadata for a project. Does not fetch file content from MinIO.
     *
     * @param projectId      the project to list documents for
     * @param authentication the current security context
     * @return list of document metadata as {@link DocumentResponse} objects
     * @throws ResourceNotFoundException if the project does not exist or is deleted
     * @throws AccessDeniedException     if the caller is not a member or owner
     */
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

    /**
     * Retrieves a document's file stream from MinIO together with its metadata.
     * The caller receives a {@link DocumentDownload} record containing the stream,
     * original filename, content type, and size for building the HTTP response.
     *
     * @param projectId      the project the document belongs to
     * @param documentId     the document to download
     * @param authentication the current security context
     * @return a {@link DocumentDownload} record with the file stream and metadata
     * @throws ResourceNotFoundException if the project or document does not exist
     * @throws AccessDeniedException     if the caller is not a member or owner
     * @throws FileStorageException      if the MinIO retrieval fails
     */
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

    /**
     * Hard-deletes a document: removes the file from MinIO and the row from the database.
     * This is intentional — hard delete immediately frees storage. Unlike projects and tasks,
     * documents are never soft-deleted.
     *
     * @param projectId      the project the document belongs to
     * @param documentId     the document to delete
     * @param authentication the current security context
     * @throws ResourceNotFoundException if the project or document does not exist
     * @throws AccessDeniedException     if the caller is not a member or owner
     * @throws FileStorageException      if the MinIO deletion fails
     */
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