package com.example.dms.service;

import com.example.dms.entity.AuditLogEntity;
import com.example.dms.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists audit log entries to the database. Uses {@code REQUIRES_NEW} transaction
 * propagation so audit inserts are committed independently of the caller's transaction
 * and survive rollbacks.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Saves a single audit log entry. Always runs in its own transaction so that
     * a caller-side rollback does not erase the audit record.
     *
     * @param actor      the email of the user who performed the action, or "anonymous"
     * @param action     the event name (e.g. "DOCUMENT_UPLOAD", "LOGIN_FAILURE")
     * @param entityType the type of entity affected (e.g. "DOCUMENT", "USER")
     * @param entityId   the string ID of the affected entity, or {@code null}
     * @param projectId  the project context of the event, or {@code null}
     * @param details    additional free-text context about the event, or {@code null}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actor, String action, String entityType,
                    String entityId, Long projectId, String details) {
        AuditLogEntity entry = new AuditLogEntity();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setProjectId(projectId);
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }
}