package com.example.dms.controller;

import com.example.dms.dto.document.DocumentDownload;
import com.example.dms.dto.document.DocumentResponse;
import com.example.dms.service.DocumentService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Endpoints for document upload, listing, download, and deletion within a project.
 * All operations require the caller to be a member or owner of the specified project.
 */
@RestController
@RequestMapping("/projects/{projectId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /** POST /projects/{projectId}/documents — uploads a file (multipart/form-data, field: "file"). Returns 201. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadDocument(projectId, file, authentication));
    }

    /** GET /projects/{projectId}/documents — lists all documents in a project. */
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> listDocuments(
            @PathVariable Long projectId,
            Authentication authentication) {
        return ResponseEntity.ok(documentService.listDocuments(projectId, authentication));
    }

    /** GET /projects/{projectId}/documents/{documentId}/download — streams the file as an attachment. */
    @GetMapping("/{documentId}/download")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable Long projectId,
            @PathVariable Long documentId,
            Authentication authentication) {
        DocumentDownload download = documentService.downloadDocument(projectId, documentId, authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.name() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(download.size()))
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(new InputStreamResource(download.stream()));
    }

    /** DELETE /projects/{projectId}/documents/{documentId} — hard-deletes the document and its file. Returns 204. */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long projectId,
            @PathVariable Long documentId,
            Authentication authentication) {
        documentService.deleteDocument(projectId, documentId, authentication);
        return ResponseEntity.noContent().build();
    }
}