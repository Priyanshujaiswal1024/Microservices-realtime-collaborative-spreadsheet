package com.collab.spreadsheet.sheet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkCellUpdateRequest {
    private List<CellUpdateItem> updates;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CellUpdateItem {
        private Integer row;
        private Integer col;
        private String value;
        private String dataType;
        private String format;
        private String lastModifiedTs;
    }
}
