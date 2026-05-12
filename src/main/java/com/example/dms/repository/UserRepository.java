package com.example.dms.repository;

import com.example.dms.entity.Role;
import com.example.dms.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRoleAndActiveTrue(Role role);
}
