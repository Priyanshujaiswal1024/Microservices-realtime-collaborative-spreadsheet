package com.collab.spreadsheet.sheet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProtectedRangeRequest {

    private String name;

    @NotNull(message = "startRow is required")
    private Integer startRow;

    @NotNull(message = "startCol is required")
    private Integer startCol;

    @NotNull(message = "endRow is required")
    private Integer endRow;

    @NotNull(message = "endCol is required")
    private Integer endCol;

    private List<String> allowedUserIds;
}
