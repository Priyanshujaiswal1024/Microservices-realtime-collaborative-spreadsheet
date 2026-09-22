package com.collab.spreadsheet.sheet.mapper;

import com.collab.spreadsheet.sheet.dto.SheetDto;
import com.collab.spreadsheet.sheet.entity.Sheet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SheetMapper {

    private final ProtectedRangeMapper protectedRangeMapper;

    public SheetDto toDto(Sheet sheet) {
        if (sheet == null) {
            return null;
        }
        return SheetDto.builder()
                .id(sheet.getId())
                .workbookId(sheet.getWorkbook() != null ? sheet.getWorkbook().getId() : null)
                .name(sheet.getName())
                .position(sheet.getPosition())
                .rowCount(sheet.getRowCount())
                .colCount(sheet.getColCount())
                .createdAt(sheet.getCreatedAt())
                .updatedAt(sheet.getUpdatedAt())
                .protectedRanges(sheet.getProtectedRanges() != null ? sheet.getProtectedRanges().stream().map(protectedRangeMapper::toDto).collect(Collectors.toList()) : null)
                .build();
    }
}
