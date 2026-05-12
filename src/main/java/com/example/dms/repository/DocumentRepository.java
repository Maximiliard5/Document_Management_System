package com.example.dms.repository;

import com.example.dms.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    List<DocumentEntity> findByProjectId(Long projectId);

    boolean existsByProjectIdAndName(Long projectId, String name);

    Optional<DocumentEntity> findByIdAndProjectId(Long id, Long projectId);
}