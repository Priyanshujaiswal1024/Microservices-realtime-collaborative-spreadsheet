package com.collab.spreadsheet.notification.service;

import com.collab.spreadsheet.common.dto.PageResponse;
import com.collab.spreadsheet.common.exception.ForbiddenException;
import com.collab.spreadsheet.common.exception.ResourceNotFoundException;
import com.collab.spreadsheet.notification.dto.NotificationDto;
import com.collab.spreadsheet.notification.entity.Notification;
import com.collab.spreadsheet.notification.mapper.NotificationMapper;
import com.collab.spreadsheet.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> getUserNotifications(String userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<NotificationDto> content = notificationMapper.toDtoList(page.getContent());

        return PageResponse.<NotificationDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationDto markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!userId.equals(notification.getUserId())) {
            throw new ForbiddenException("Cannot modify notifications of other users");
        }

        notification.setRead(true);
        notification.setReadAt(Instant.now());
        notification = notificationRepository.save(notification);

        return notificationMapper.toDto(notification);
    }

    @Transactional
    public int markAllAsRead(String userId) {
        return notificationRepository.markAllAsReadForUser(userId, Instant.now());
    }
}
