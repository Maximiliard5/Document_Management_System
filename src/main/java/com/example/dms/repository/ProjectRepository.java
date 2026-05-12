package com.example.dms.repository;

import com.example.dms.entity.ProjectEntity;
import com.example.dms.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    @Query("SELECT p FROM ProjectEntity p WHERE (p.owner = :user OR :user MEMBER OF p.members) AND p.deleted = false")
    List<ProjectEntity> findAllByUser(@Param("user") UserEntity user);
}
