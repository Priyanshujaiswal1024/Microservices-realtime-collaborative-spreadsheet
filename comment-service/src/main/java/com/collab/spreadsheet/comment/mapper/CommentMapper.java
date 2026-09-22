package com.collab.spreadsheet.comment.mapper;

import com.collab.spreadsheet.comment.dto.CommentReplyDto;
import com.collab.spreadsheet.comment.dto.CommentThreadDto;
import com.collab.spreadsheet.comment.entity.CommentReply;
import com.collab.spreadsheet.comment.entity.CommentThread;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommentMapper {

    public CommentThreadDto toDto(CommentThread entity) {
        if (entity == null) {
            return null;
        }
        return CommentThreadDto.builder()
                .id(entity.getId())
                .workbookId(entity.getWorkbookId())
                .sheetId(entity.getSheetId())
                .row(entity.getRow())
                .col(entity.getCol())
                .authorId(entity.getAuthorId())
                .initialContent(entity.getInitialContent())
                .resolved(entity.getResolved())
                .resolvedBy(entity.getResolvedBy())
                .resolvedAt(entity.getResolvedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .replies(entity.getReplies() != null ? toReplyDtoList(entity.getReplies()) : Collections.emptyList())
                .build();
    }

    public List<CommentThreadDto> toDtoList(List<CommentThread> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    public CommentReplyDto toDto(CommentReply entity) {
        if (entity == null) {
            return null;
        }
        return CommentReplyDto.builder()
                .id(entity.getId())
                .threadId(entity.getThread() != null ? entity.getThread().getId() : null)
                .authorId(entity.getAuthorId())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public List<CommentReplyDto> toReplyDtoList(List<CommentReply> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
