package com.collab.spreadsheet.notification.service;

import com.collab.spreadsheet.common.dto.PageResponse;
import com.collab.spreadsheet.notification.dto.NotificationDto;
import com.collab.spreadsheet.notification.entity.Notification;
import com.collab.spreadsheet.notification.mapper.NotificationMapper;
import com.collab.spreadsheet.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private Notification sampleNotification;
    private NotificationDto sampleNotificationDto;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .id("notif-1")
                .userId("user-1")
                .actorId("user-2")
                .type("COMMENT_MENTION")
                .title("Mentioned in a comment")
                .message("User user-2 mentioned you")
                .read(false)
                .createdAt(Instant.now())
                .build();

        sampleNotificationDto = NotificationDto.builder()
                .id("notif-1")
                .userId("user-1")
                .actorId("user-2")
                .type("COMMENT_MENTION")
                .title("Mentioned in a comment")
                .message("User user-2 mentioned you")
                .read(false)
                .createdAt(sampleNotification.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("Should retrieve paginated user notifications")
    void testGetUserNotifications() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(sampleNotification), pageable, 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("user-1", pageable)).thenReturn(page);
        when(notificationMapper.toDtoList(List.of(sampleNotification))).thenReturn(List.of(sampleNotificationDto));

        PageResponse<NotificationDto> response = notificationService.getUserNotifications("user-1", pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("Mentioned in a comment");
    }

    @Test
    @DisplayName("Should mark notification as read")
    void testMarkAsRead() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(sampleNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(sampleNotificationDto);

        NotificationDto result = notificationService.markAsRead("notif-1", "user-1");

        assertThat(result).isNotNull();
        assertThat(sampleNotification.getRead()).isTrue();
        assertThat(sampleNotification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("Should get unread notifications count")
    void testGetUnreadCount() {
        when(notificationRepository.countByUserIdAndReadFalse("user-1")).thenReturn(5L);

        long count = notificationService.getUnreadCount("user-1");

        assertThat(count).isEqualTo(5L);
    }
}
