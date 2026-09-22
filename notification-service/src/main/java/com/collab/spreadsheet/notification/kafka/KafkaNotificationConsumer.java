package com.collab.spreadsheet.notification.kafka;

import com.collab.spreadsheet.common.config.KafkaTopics;
import com.collab.spreadsheet.common.events.CommentEvent;
import com.collab.spreadsheet.common.events.NotificationEvent;
import com.collab.spreadsheet.common.events.SheetLifecycleEvent;
import com.collab.spreadsheet.common.events.UserEvent;
import com.collab.spreadsheet.notification.entity.Notification;
import com.collab.spreadsheet.notification.repository.NotificationRepository;
import com.collab.spreadsheet.notification.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;
    private final StringRedisTemplate redisTemplate;

    private static final String DEDUPE_PREFIX = "notification:dedupe:";
    private static final Duration DEDUPE_TTL = Duration.ofHours(24);

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "notification-service-user-events")
    @Transactional
    public void consumeUserEvent(UserEvent event) {
        if (event == null || event.getEmail() == null) {
            return;
        }

        if (event.getEventId() != null) {
            String dedupeKey = DEDUPE_PREFIX + event.getEventId();
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", DEDUPE_TTL);
            if (Boolean.FALSE.equals(isNew)) {
                log.debug("Duplicate UserEvent skipped: {}", event.getEventId());
                return;
            }
        }

        log.info("Received UserEvent: type={}, email={}, purpose={}", event.getType(), event.getEmail(), event.getPurpose());

        if (event.getType() == UserEvent.Type.OTP_GENERATED) {
            String subject = "PASSWORD_RESET".equalsIgnoreCase(event.getPurpose()) 
                    ? "Password Reset Code - Real-Time Spreadsheet" 
                    : "Email Verification Code - Real-Time Spreadsheet";
            
            String htmlBody = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:20px;border:1px solid #e0e0e0;border-radius:8px;'>"
                    + "<h2 style='color:#1a73e8;'>Spreadsheet Platform</h2>"
                    + "<p>Hello,</p>"
                    + "<p>Your 6-digit verification code is:</p>"
                    + "<div style='font-size:32px;font-weight:bold;letter-spacing:6px;color:#1a73e8;background:#f1f3f4;padding:15px;text-align:center;border-radius:6px;margin:20px 0;'>"
                    + event.getOtp()
                    + "</div>"
                    + "<p>This code is valid for <strong>5 minutes</strong>. If you did not request this code, please ignore this email.</p>"
                    + "<hr style='border:none;border-top:1px solid #eee;margin:20px 0;'/>"
                    + "<p style='font-size:12px;color:#777;'>Real-Time Collaborative Spreadsheet Platform</p>"
                    + "</div>";

            emailNotificationService.sendEmailNotification(event.getEmail(), subject, htmlBody);
        }
    }

    @KafkaListener(topics = KafkaTopics.NOTIFICATIONS, groupId = "notification-service-notifications")
    @Transactional
    public void consumeNotification(NotificationEvent event) {
        if (event == null || event.getUserId() == null) {
            return;
        }

        if (event.getEventId() != null) {
            String dedupeKey = DEDUPE_PREFIX + event.getEventId();
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", DEDUPE_TTL);
            if (Boolean.FALSE.equals(isNew)) {
                log.debug("Duplicate NotificationEvent skipped: {}", event.getEventId());
                return;
            }
        }

        log.info("Received notification event for user {}: type={}, title='{}'", 
                event.getUserId(), event.getType(), event.getTitle());

        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .actorId(event.getActorId())
                .type(event.getType() != null ? event.getType().name() : "GENERAL")
                .title(event.getTitle())
                .message(event.getMessage())
                .link(event.getLink())
                .workbookId(event.getWorkbookId())
                .sheetId(event.getSheetId())
                .read(false)
                .build();

        notificationRepository.save(notification);

        emailNotificationService.sendEmailNotification(
                event.getUserId(),
                event.getTitle(),
                event.getMessage()
        );
    }

    @KafkaListener(topics = KafkaTopics.COMMENTS, groupId = "notification-service-comments")
    @Transactional
    public void consumeCommentEvent(CommentEvent event) {
        if (event == null || event.getEventId() == null) {
            return;
        }

        String dedupeKey = DEDUPE_PREFIX + event.getEventId();
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", DEDUPE_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.debug("Duplicate CommentEvent skipped in notification-service: {}", event.getEventId());
            return;
        }

        log.info("Processing CommentEvent for notifications: action={}, commentId={}", event.getAction(), event.getCommentId());

        if (event.getMentionedUserIds() != null) {
            for (String mentionedUser : event.getMentionedUserIds()) {
                Notification notif = Notification.builder()
                        .userId(mentionedUser)
                        .actorId(event.getActorId())
                        .type("COMMENT_MENTION")
                        .title("You were mentioned in a comment")
                        .message("User " + event.getActorId() + " mentioned you: " + (event.getText() != null ? event.getText() : ""))
                        .workbookId(event.getWorkbookId())
                        .sheetId(event.getSheetId())
                        .read(false)
                        .build();
                notificationRepository.save(notif);
            }
        }
    }

    @KafkaListener(topics = KafkaTopics.SHEET_LIFECYCLE, groupId = "notification-service-sheet-lifecycle")
    @Transactional
    public void consumeSheetLifecycle(SheetLifecycleEvent event) {
        if (event == null || event.getEventId() == null) {
            return;
        }

        String dedupeKey = DEDUPE_PREFIX + event.getEventId();
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", DEDUPE_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.debug("Duplicate SheetLifecycleEvent skipped in notification-service: {}", event.getEventId());
            return;
        }

        log.info("Processing SheetLifecycleEvent for notifications: action={}, workbookId={}", event.getAction(), event.getWorkbookId());

        if ((event.getAction() == SheetLifecycleEvent.Action.SHARED_WITH_USER || 
             event.getAction() == SheetLifecycleEvent.Action.PERMISSION_GRANTED) && event.getTargetUserId() != null) {
            Notification notif = Notification.builder()
                    .userId(event.getTargetUserId())
                    .actorId(event.getActorId())
                    .type("WORKBOOK_SHARED")
                    .title("Workbook shared with you")
                    .message("User " + (event.getActorId() != null ? event.getActorId() : "A collaborator") + " shared a workbook with you")
                    .workbookId(event.getWorkbookId())
                    .sheetId(event.getSheetId())
                    .read(false)
                    .build();
            notificationRepository.save(notif);

            // Dispatch Invitation Email via SMTP
            if (event.getTargetUserId().contains("@")) {
                String shareUrl = "http://localhost:5173/?wb=" + event.getWorkbookId();
                String emailBody = "<p>Hello,</p>"
                        + "<p><strong>" + (event.getActorId() != null ? event.getActorId() : "A collaborator") + "</strong> has invited you to collaborate on a spreadsheet workbook.</p>"
                        + "<div style='margin: 25px 0;'><a href='" + shareUrl + "' style='background-color:#10b981;color:#ffffff;text-decoration:none;padding:12px 24px;border-radius:6px;font-weight:bold;font-size:14px;display:inline-block;'>Open Shared Spreadsheet</a></div>"
                        + "<p style='font-size:13px;color:#64748b;'>Direct Link: <a href='" + shareUrl + "' style='color:#10b981;'>" + shareUrl + "</a></p>";

                emailNotificationService.sendEmailNotification(
                        event.getTargetUserId(),
                        "Spreadsheet Invitation: You've been invited to collaborate",
                        emailBody
                );
            }
        }
    }
}
