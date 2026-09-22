package com.collab.spreadsheet.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${notifications.email.from:priyanshjais123@gmail.com}")
    private String fromEmail;

    @Value("${notifications.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Send email notification asynchronously via JavaMailSender
     */
    @Async
    public void sendEmailNotification(String toEmail, String subject, String body) {
        if (!emailEnabled) {
            log.debug("Email notifications are disabled by configuration. Skipping email to '{}'", toEmail);
            return;
        }

        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Cannot send email: recipient address is empty");
            return;
        }

        // If recipient is just a username, format as fallback
        String recipient = toEmail.contains("@") ? toEmail : toEmail + "@example.com";

        log.info("Sending Email via JavaMailSender to '{}': Subject='{}'", recipient, subject);

        if (mailSender == null) {
            log.warn("JavaMailSender bean not configured. Falling back to log output: to={}, subject={}", recipient, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "SheetForge Collaboration Platform");
            helper.setTo(recipient);
            helper.setSubject("[SheetForge] " + subject);

            String htmlContent = buildHtmlEmailTemplate(subject, body);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email notification successfully sent to '{}'", recipient);

        } catch (Exception e) {
            log.error("Failed to send email to '{}' via Google SMTP: {}", recipient, e.getMessage(), e);
        }
    }

    /**
     * Clean modern HTML email template
     */
    private String buildHtmlEmailTemplate(String title, String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; margin: 0; padding: 20px; color: #334155; }
                    .container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.2); }
                    .header { background: #0f172a; padding: 24px; text-align: center; border-bottom: 2px solid #10b981; }
                    .logo { font-size: 20px; font-weight: bold; color: #ffffff; letter-spacing: -0.5px; }
                    .logo span { color: #10b981; }
                    .content { padding: 32px 28px; }
                    .title { font-size: 18px; font-weight: 700; color: #0f172a; margin-bottom: 16px; }
                    .message { font-size: 14px; line-height: 1.6; color: #475569; background: #f8fafc; padding: 16px; border-left: 4px solid #10b981; border-radius: 4px; margin-bottom: 24px; }
                    .btn { display: inline-block; background-color: #10b981; color: #ffffff; text-decoration: none; padding: 10px 22px; border-radius: 8px; font-weight: 600; font-size: 14px; }
                    .footer { background: #f1f5f9; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">Sheet<span>Forge</span> Real-Time Collaboration</div>
                    </div>
                    <div class="content">
                        <div class="title">%s</div>
                        <div class="message">%s</div>
                        <a href="http://localhost:5173" class="btn">Open Spreadsheet</a>
                    </div>
                    <div class="footer">
                        Sent by SheetForge Platform. You received this because you are a collaborator on this workbook.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(title, message);
    }
}
