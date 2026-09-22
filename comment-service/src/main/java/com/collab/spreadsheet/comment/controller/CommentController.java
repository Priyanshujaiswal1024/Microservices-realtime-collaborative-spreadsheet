package com.collab.spreadsheet.comment.controller;

import com.collab.spreadsheet.comment.dto.*;
import com.collab.spreadsheet.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/workbooks/{workbookId}/comments")
    public ResponseEntity<List<CommentThreadDto>> getWorkbookComments(@PathVariable String workbookId) {
        return ResponseEntity.ok(commentService.getWorkbookComments(workbookId));
    }

    @GetMapping("/sheets/{sheetId}/comments")
    public ResponseEntity<List<CommentThreadDto>> getSheetComments(@PathVariable String sheetId) {
        return ResponseEntity.ok(commentService.getSheetComments(sheetId));
    }

    @PostMapping("/sheets/{sheetId}/comments")
    public ResponseEntity<CommentThreadDto> createComment(
            @PathVariable String sheetId,
            @Valid @RequestBody CreateCommentRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        CommentThreadDto thread = commentService.createCommentThread(sheetId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(thread);
    }

    @PostMapping("/comments/{threadId}/replies")
    public ResponseEntity<CommentReplyDto> addReply(
            @PathVariable String threadId,
            @Valid @RequestBody ReplyCommentRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        CommentReplyDto reply = commentService.addReply(threadId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(reply);
    }

    @PutMapping("/comments/{threadId}/resolve")
    public ResponseEntity<CommentThreadDto> resolveThread(
            @PathVariable String threadId,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        CommentThreadDto resolved = commentService.resolveThread(threadId, userId);
        return ResponseEntity.ok(resolved);
    }

    @DeleteMapping("/comments/{threadId}")
    public ResponseEntity<Map<String, String>> deleteThread(
            @PathVariable String threadId,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        commentService.deleteThread(threadId, userId);
        return ResponseEntity.ok(Map.of("message", "Comment deleted successfully"));
    }
}
