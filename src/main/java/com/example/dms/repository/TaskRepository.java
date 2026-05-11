package com.example.dms.repository;

import com.example.dms.entity.TaskEntity;
import com.example.dms.entity.TaskPriority;
import com.example.dms.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    List<TaskEntity> findByProjectIdAndDeletedFalse(Long projectId);

    List<TaskEntity> findByProjectIdAndStatusAndDeletedFalse(Long projectId, TaskStatus status);

    List<TaskEntity> findByProjectIdAndPriorityAndDeletedFalse(Long projectId, TaskPriority priority);

    List<TaskEntity> findByProjectIdAndStatusAndPriorityAndDeletedFalse(Long projectId, TaskStatus status, TaskPriority priority);
}