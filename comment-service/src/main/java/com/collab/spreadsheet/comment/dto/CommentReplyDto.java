package com.collab.spreadsheet.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentReplyDto {
    private String id;
    private String threadId;
    private String authorId;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
