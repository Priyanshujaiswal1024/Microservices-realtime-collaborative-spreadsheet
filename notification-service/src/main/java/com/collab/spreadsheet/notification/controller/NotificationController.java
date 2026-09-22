package com.collab.spreadsheet.notification.controller;

import com.collab.spreadsheet.common.dto.PageResponse;
import com.collab.spreadsheet.notification.dto.NotificationDto;
import com.collab.spreadsheet.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.collab.spreadsheet.notification.service.EmailNotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final EmailNotificationService emailNotificationService;

    @PostMapping("/invite")
    public ResponseEntity<Map<String, String>> sendInvite(
            @RequestBody Map<String, String> payload) {
        String recipientEmail = payload.get("userEmail") != null ? payload.get("userEmail") : payload.get("toEmail");
        String inviter = payload.get("actorId") != null ? payload.get("actorId") : "A collaborator";
        String workbookId = payload.get("workbookId");

        if (recipientEmail != null && recipientEmail.contains("@")) {
            String shareUrl = "http://localhost:5173/?wb=" + workbookId;
            String emailBody = "<p>Hello,</p>"
                    + "<p><strong>" + inviter + "</strong> has invited you to collaborate on a spreadsheet workbook.</p>"
                    + "<div style='margin: 25px 0;'><a href='" + shareUrl + "' style='background-color:#10b981;color:#ffffff;text-decoration:none;padding:12px 24px;border-radius:6px;font-weight:bold;font-size:14px;display:inline-block;'>Open Shared Spreadsheet</a></div>"
                    + "<p style='font-size:13px;color:#64748b;'>Direct Link: <a href='" + shareUrl + "' style='color:#10b981;'>" + shareUrl + "</a></p>";

            log.info("Direct REST trigger for invitation email to {}", recipientEmail);
            emailNotificationService.sendEmailNotification(
                    recipientEmail,
                    "Spreadsheet Invitation: You've been invited to collaborate",
                    emailBody
            );
            return ResponseEntity.ok(Map.of("status", "SENT", "recipient", recipientEmail));
        }
        return ResponseEntity.badRequest().body(Map.of("status", "INVALID_EMAIL"));
    }

    @GetMapping
    public ResponseEntity<PageResponse<NotificationDto>> getUserNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        NotificationDto notification = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        int updated = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "Marked all notifications as read", "count", updated));
    }
}
