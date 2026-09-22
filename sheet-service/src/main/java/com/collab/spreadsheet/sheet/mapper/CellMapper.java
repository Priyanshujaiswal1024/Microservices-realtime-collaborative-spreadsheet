package com.collab.spreadsheet.sheet.mapper;

import com.collab.spreadsheet.sheet.dto.CellDto;
import com.collab.spreadsheet.sheet.entity.Cell;
import org.springframework.stereotype.Component;

@Component
public class CellMapper {

    public CellDto toDto(Cell cell) {
        if (cell == null) {
            return null;
        }
        return CellDto.builder()
                .id(cell.getId())
                .sheetId(cell.getSheet() != null ? cell.getSheet().getId() : null)
                .row(cell.getRow())
                .col(cell.getCol())
                .value(cell.getValue())
                .format(cell.getFormat())
                .dataType(cell.getDataType())
                .updatedAt(cell.getUpdatedAt())
                .build();
    }
}
