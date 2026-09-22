package com.collab.spreadsheet.sheet.dto;

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
public class SheetDto {
    private String id;
    private String workbookId;
    private String name;
    private Integer position;
    private Integer rowCount;
    private Integer colCount;
    private List<ProtectedRangeDto> protectedRanges;
    private Instant createdAt;
    private Instant updatedAt;
}
