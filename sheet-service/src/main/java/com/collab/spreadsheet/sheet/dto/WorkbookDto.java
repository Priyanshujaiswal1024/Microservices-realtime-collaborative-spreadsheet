package com.collab.spreadsheet.sheet.dto;

import com.collab.spreadsheet.common.dto.PermissionRole;
import com.collab.spreadsheet.common.dto.WorkbookVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkbookDto {
    private String id;
    private String ownerId;
    private String title;
    private WorkbookVisibility visibility;
    private PermissionRole userRole; // Current requesting user's role
    private List<SheetDto> sheets;
    private List<PermissionDto> permissions;
    private Instant createdAt;
    private Instant updatedAt;
}
