package com.example.dms.service;

import com.example.dms.entity.AuditLogEntity;
import com.example.dms.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

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