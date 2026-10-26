package com.collab.spreadsheet.sheet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveWorkbookExportRequest {
    private String title;
    private List<LiveSheetExportDto> sheets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveSheetExportDto {
        private String name;
        private List<LiveCellExportDto> cells;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveCellExportDto {
        private Integer row;
        private Integer col;
        private String value;
        private String dataType;
        private Map<String, Object> format;
    }
}
