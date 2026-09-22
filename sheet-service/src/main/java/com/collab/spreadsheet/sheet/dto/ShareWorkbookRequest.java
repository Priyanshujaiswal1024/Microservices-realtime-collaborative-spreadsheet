package com.collab.spreadsheet.sheet.dto;

import com.collab.spreadsheet.common.dto.PermissionRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareWorkbookRequest {

    private String userId;

    private String userEmail;

    @NotNull(message = "Role is required")
    private PermissionRole role;
}
