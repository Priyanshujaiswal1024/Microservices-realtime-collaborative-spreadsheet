package com.collab.spreadsheet.sheet.dto;

import com.collab.spreadsheet.common.dto.PermissionRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionDto {
    private String id;
    private String workbookId;
    private String userId;
    private String userEmail;
    private PermissionRole role;
    private Instant createdAt;
    private Instant updatedAt;
}
