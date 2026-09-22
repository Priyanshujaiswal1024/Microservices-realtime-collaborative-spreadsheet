package com.collab.spreadsheet.sheet.mapper;

import com.collab.spreadsheet.sheet.dto.WorkbookDto;
import com.collab.spreadsheet.sheet.entity.Workbook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WorkbookMapper {

    private final SheetMapper sheetMapper;
    private final PermissionMapper permissionMapper;

    public WorkbookDto toDto(Workbook workbook) {
        if (workbook == null) {
            return null;
        }
        return WorkbookDto.builder()
                .id(workbook.getId())
                .ownerId(workbook.getOwnerId())
                .title(workbook.getTitle())
                .visibility(workbook.getVisibility())
                .createdAt(workbook.getCreatedAt())
                .updatedAt(workbook.getUpdatedAt())
                .sheets(workbook.getSheets() != null ? workbook.getSheets().stream().map(sheetMapper::toDto).collect(Collectors.toList()) : null)
                .permissions(workbook.getPermissions() != null ? workbook.getPermissions().stream().map(permissionMapper::toDto).collect(Collectors.toList()) : null)
                .build();
    }
}
