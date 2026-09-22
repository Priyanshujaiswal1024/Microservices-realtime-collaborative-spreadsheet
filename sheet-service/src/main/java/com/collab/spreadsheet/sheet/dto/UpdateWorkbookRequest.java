package com.collab.spreadsheet.sheet.dto;

import com.collab.spreadsheet.common.dto.WorkbookVisibility;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWorkbookRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private WorkbookVisibility visibility;
}
