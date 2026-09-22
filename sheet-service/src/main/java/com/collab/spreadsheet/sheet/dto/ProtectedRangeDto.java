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
public class ProtectedRangeDto {
    private String id;
    private String sheetId;
    private String name;
    private Integer startRow;
    private Integer startCol;
    private Integer endRow;
    private Integer endCol;
    private List<String> allowedUserIds;
    private Instant createdAt;
}
