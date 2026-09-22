package com.collab.spreadsheet.sheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSheetRequest {

    @NotBlank(message = "Sheet name is required")
    @Size(max = 100, message = "Sheet name must not exceed 100 characters")
    private String name;

    private Integer rowCount;

    private Integer colCount;
}
