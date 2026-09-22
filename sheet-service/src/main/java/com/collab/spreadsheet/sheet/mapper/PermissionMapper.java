package com.collab.spreadsheet.sheet.mapper;

import com.collab.spreadsheet.sheet.dto.PermissionDto;
import com.collab.spreadsheet.sheet.entity.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    public PermissionDto toDto(Permission permission) {
        if (permission == null) {
            return null;
        }
        return PermissionDto.builder()
                .id(permission.getId())
                .workbookId(permission.getWorkbook() != null ? permission.getWorkbook().getId() : null)
                .userId(permission.getUserId())
                .userEmail(permission.getUserEmail())
                .role(permission.getRole())
                .createdAt(permission.getCreatedAt())
                .build();
    }
}
