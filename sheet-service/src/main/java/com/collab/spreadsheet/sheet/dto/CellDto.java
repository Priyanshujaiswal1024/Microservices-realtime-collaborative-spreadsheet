package com.collab.spreadsheet.sheet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CellDto {
    private String id;
    private String sheetId;
    private Integer row;
    private Integer col;
    private String value;
    private String dataType;
    private String format;
    private String lastModifiedTs;
    private String lastModifiedBy;
    private Long version;
    private Instant updatedAt;
}
