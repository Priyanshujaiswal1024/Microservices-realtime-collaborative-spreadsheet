package com.collab.spreadsheet.comment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyCommentRequest {

    @NotBlank(message = "Reply content cannot be blank")
    private String content;
}
