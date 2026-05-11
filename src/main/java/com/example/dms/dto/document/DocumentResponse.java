package com.example.dms.dto.document;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private String name;
    private String type;
    private Long size;
    private Long projectId;
    private Long ownerId;
    private LocalDateTime createdAt;
}