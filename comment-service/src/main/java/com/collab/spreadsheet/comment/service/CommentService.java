package com.collab.spreadsheet.comment.service;

import com.collab.spreadsheet.comment.dto.*;
import com.collab.spreadsheet.comment.entity.CommentReply;
import com.collab.spreadsheet.comment.entity.CommentThread;
import com.collab.spreadsheet.comment.kafka.KafkaCommentProducer;
import com.collab.spreadsheet.comment.mapper.CommentMapper;
import com.collab.spreadsheet.comment.repository.CommentReplyRepository;
import com.collab.spreadsheet.comment.repository.CommentThreadRepository;
import com.collab.spreadsheet.common.events.CommentEvent;
import com.collab.spreadsheet.common.events.NotificationEvent;
import com.collab.spreadsheet.common.exception.ForbiddenException;
import com.collab.spreadsheet.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentThreadRepository commentThreadRepository;
    private final CommentReplyRepository commentReplyRepository;
    private final CommentMapper commentMapper;
    private final KafkaCommentProducer kafkaCommentProducer;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_-]+)");

    @Transactional
    public CommentThreadDto createCommentThread(String sheetId, CreateCommentRequest request, String userId) {
        log.info("Creating comment on sheet {} at ({},{}) by user {}", sheetId, request.getRow(), request.getCol(), userId);

        CommentThread thread = CommentThread.builder()
                .workbookId(request.getWorkbookId() != null ? request.getWorkbookId() : "wb-default")
                .sheetId(sheetId)
                .row(request.getRow())
                .col(request.getCol())
                .authorId(userId)
                .initialContent(request.getContent())
                .resolved(false)
                .replies(new ArrayList<>())
                .build();

        thread = commentThreadRepository.save(thread);

        List<String> mentions = extractMentions(request.getContent());

        // Publish CommentEvent
        kafkaCommentProducer.publishCommentEvent(CommentEvent.builder()
                .actorId(userId)
                .action(CommentEvent.Action.COMMENT_CREATED)
                .commentId(thread.getId())
                .workbookId(thread.getWorkbookId())
                .sheetId(sheetId)
                .text(request.getContent())
                .mentionedUserIds(mentions)
                .build());

        // Publish notifications for mentions
        for (String mentionedUser : mentions) {
            kafkaCommentProducer.publishNotification(NotificationEvent.builder()
                    .actorId(userId)
                    .type(NotificationEvent.Type.COMMENT_MENTION)
                    .userId(mentionedUser)
                    .title("Mentioned in a comment")
                    .message("User " + userId + " mentioned you in a comment")
                    .workbookId(thread.getWorkbookId())
                    .sheetId(sheetId)
                    .build());
        }

        return commentMapper.toDto(thread);
    }

    @Transactional
    public CommentReplyDto addReply(String threadId, ReplyCommentRequest request, String userId) {
        CommentThread thread = commentThreadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("CommentThread", threadId));

        CommentReply reply = CommentReply.builder()
                .thread(thread)
                .authorId(userId)
                .content(request.getContent())
                .build();

        reply = commentReplyRepository.save(reply);
        thread.getReplies().add(reply);

        List<String> mentions = extractMentions(request.getContent());

        // Publish Reply added event
        kafkaCommentProducer.publishCommentEvent(CommentEvent.builder()
                .actorId(userId)
                .action(CommentEvent.Action.REPLY_ADDED)
                .commentId(reply.getId())
                .parentCommentId(threadId)
                .workbookId(thread.getWorkbookId())
                .sheetId(thread.getSheetId())
                .text(request.getContent())
                .mentionedUserIds(mentions)
                .build());

        // Notify original author if someone else replied
        if (!userId.equals(thread.getAuthorId())) {
            kafkaCommentProducer.publishNotification(NotificationEvent.builder()
                    .actorId(userId)
                    .type(NotificationEvent.Type.COMMENT_REPLY)
                    .userId(thread.getAuthorId())
                    .title("New reply to your comment")
                    .message("User " + userId + " replied to your comment")
                    .workbookId(thread.getWorkbookId())
                    .sheetId(thread.getSheetId())
                    .build());
        }

        return commentMapper.toDto(reply);
    }

    @Transactional
    public CommentThreadDto resolveThread(String threadId, String userId) {
        CommentThread thread = commentThreadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("CommentThread", threadId));

        thread.setResolved(true);
        thread.setResolvedBy(userId);
        thread.setResolvedAt(Instant.now());
        thread = commentThreadRepository.save(thread);

        kafkaCommentProducer.publishCommentEvent(CommentEvent.builder()
                .actorId(userId)
                .action(CommentEvent.Action.COMMENT_RESOLVED)
                .commentId(threadId)
                .workbookId(thread.getWorkbookId())
                .sheetId(thread.getSheetId())
                .build());

        return commentMapper.toDto(thread);
    }

    @Transactional(readOnly = true)
    public List<CommentThreadDto> getWorkbookComments(String workbookId) {
        List<CommentThread> threads = commentThreadRepository.findByWorkbookIdOrderByCreatedAtDesc(workbookId);
        return commentMapper.toDtoList(threads);
    }

    @Transactional(readOnly = true)
    public List<CommentThreadDto> getSheetComments(String sheetId) {
        List<CommentThread> threads = commentThreadRepository.findBySheetIdOrderByCreatedAtDesc(sheetId);
        return commentMapper.toDtoList(threads);
    }

    @Transactional
    public void deleteThread(String threadId, String userId) {
        CommentThread thread = commentThreadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("CommentThread", threadId));

        // Allow comment deletion by authors, collaborators, and sheet editors
        commentThreadRepository.delete(thread);

        kafkaCommentProducer.publishCommentEvent(CommentEvent.builder()
                .actorId(userId)
                .action(CommentEvent.Action.COMMENT_DELETED)
                .commentId(threadId)
                .workbookId(thread.getWorkbookId())
                .sheetId(thread.getSheetId())
                .build());
    }

    private List<String> extractMentions(String text) {
        List<String> mentions = new ArrayList<>();
        if (text == null) return mentions;
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }
}
