package com.collab.spreadsheet.comment.service;

import com.collab.spreadsheet.comment.dto.CommentReplyDto;
import com.collab.spreadsheet.comment.dto.CommentThreadDto;
import com.collab.spreadsheet.comment.dto.CreateCommentRequest;
import com.collab.spreadsheet.comment.dto.ReplyCommentRequest;
import com.collab.spreadsheet.comment.entity.CommentReply;
import com.collab.spreadsheet.comment.entity.CommentThread;
import com.collab.spreadsheet.comment.kafka.KafkaCommentProducer;
import com.collab.spreadsheet.comment.mapper.CommentMapper;
import com.collab.spreadsheet.comment.repository.CommentReplyRepository;
import com.collab.spreadsheet.comment.repository.CommentThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentThreadRepository commentThreadRepository;

    @Mock
    private CommentReplyRepository commentReplyRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private KafkaCommentProducer kafkaCommentProducer;

    @InjectMocks
    private CommentService commentService;

    private CommentThread sampleThread;
    private CommentThreadDto sampleThreadDto;

    @BeforeEach
    void setUp() {
        sampleThread = CommentThread.builder()
                .id("th-1")
                .workbookId("wb-1")
                .sheetId("sheet-1")
                .row(1)
                .col(2)
                .authorId("user-1")
                .initialContent("Please check this formula @user-2")
                .resolved(false)
                .replies(new ArrayList<>())
                .build();

        sampleThreadDto = CommentThreadDto.builder()
                .id("th-1")
                .workbookId("wb-1")
                .sheetId("sheet-1")
                .row(1)
                .col(2)
                .authorId("user-1")
                .initialContent("Please check this formula @user-2")
                .resolved(false)
                .build();
    }

    @Test
    @DisplayName("Should create comment thread and publish notification for @mentions")
    void testCreateCommentWithMention() {
        CreateCommentRequest request = CreateCommentRequest.builder()
                .workbookId("wb-1")
                .row(1)
                .col(2)
                .content("Please check this formula @user-2")
                .build();

        when(commentThreadRepository.save(any(CommentThread.class))).thenReturn(sampleThread);
        when(commentMapper.toDto(sampleThread)).thenReturn(sampleThreadDto);

        CommentThreadDto result = commentService.createCommentThread("sheet-1", request, "user-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("th-1");
        verify(commentThreadRepository).save(any(CommentThread.class));
        verify(kafkaCommentProducer).publishCommentEvent(any());
        verify(kafkaCommentProducer).publishNotification(any()); // Triggered for @user-2
    }

    @Test
    @DisplayName("Should add reply to thread and notify original thread author")
    void testAddReply() {
        ReplyCommentRequest request = ReplyCommentRequest.builder()
                .content("Done, formula corrected!")
                .build();

        CommentReply reply = CommentReply.builder()
                .id("rep-1")
                .thread(sampleThread)
                .authorId("user-2")
                .content("Done, formula corrected!")
                .build();

        CommentReplyDto replyDto = CommentReplyDto.builder()
                .id("rep-1")
                .authorId("user-2")
                .content("Done, formula corrected!")
                .build();

        when(commentThreadRepository.findById("th-1")).thenReturn(Optional.of(sampleThread));
        when(commentReplyRepository.save(any(CommentReply.class))).thenReturn(reply);
        when(commentMapper.toDto(reply)).thenReturn(replyDto);

        CommentReplyDto result = commentService.addReply("th-1", request, "user-2");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Done, formula corrected!");
        verify(kafkaCommentProducer).publishCommentEvent(any());
        verify(kafkaCommentProducer).publishNotification(any()); // Notified user-1
    }

    @Test
    @DisplayName("Should resolve comment thread")
    void testResolveThread() {
        when(commentThreadRepository.findById("th-1")).thenReturn(Optional.of(sampleThread));
        when(commentThreadRepository.save(sampleThread)).thenReturn(sampleThread);
        when(commentMapper.toDto(sampleThread)).thenReturn(sampleThreadDto);

        CommentThreadDto result = commentService.resolveThread("th-1", "user-1");

        assertThat(result).isNotNull();
        assertThat(sampleThread.getResolved()).isTrue();
        verify(kafkaCommentProducer).publishCommentEvent(any());
    }
}
