package com.collab.spreadsheet.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentThreadDto {
    private String id;
    private String workbookId;
    private String sheetId;
    private Integer row;
    private Integer col;
    private String authorId;
    private String initialContent;
    private Boolean resolved;
    private String resolvedBy;
    private Instant resolvedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CommentReplyDto> replies;
}
