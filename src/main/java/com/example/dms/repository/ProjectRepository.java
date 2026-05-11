package com.example.dms.repository;

import com.example.dms.entity.Project;
import com.example.dms.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwner(UserEntity owner);

    @Query("SELECT p FROM Project p JOIN p.members m WHERE m = :user")
    List<Project> findByMember(@Param("user") UserEntity user);

    @Query("SELECT p FROM Project p WHERE p.owner = :user OR :user MEMBER OF p.members")
    List<Project> findAllByUser(@Param("user") UserEntity user);
}
