package com.example.dms.repository;

import com.example.dms.entity.ProjectEntity;
import com.example.dms.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    List<ProjectEntity> findByOwner(UserEntity owner);

    @Query("SELECT p FROM ProjectEntity p JOIN p.members m WHERE m = :user")
    List<ProjectEntity> findByMember(@Param("user") UserEntity user);

    @Query("SELECT p FROM ProjectEntity p WHERE p.owner = :user OR :user MEMBER OF p.members")
    List<ProjectEntity> findAllByUser(@Param("user") UserEntity user);
}
