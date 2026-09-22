package com.collab.spreadsheet.audit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSnapshotRequest {

    @NotBlank(message = "Snapshot label is required")
    private String label;

    private String description;

    @NotBlank(message = "Snapshot data payload is required")
    private String snapshotData;
}
