package com.collab.spreadsheet.comment.dto;

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
public class CreateCommentRequest {

    private String workbookId;

    @NotNull(message = "Row coordinate is required")
    private Integer row;

    @NotNull(message = "Column coordinate is required")
    private Integer col;

    @NotBlank(message = "Comment content cannot be blank")
    private String content;
}
