package com.collab.spreadsheet.sheet.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSheetRequest {

    @Size(max = 100, message = "Sheet name must not exceed 100 characters")
    private String name;

    private Integer position;

    private Integer rowCount;

    private Integer colCount;
}
