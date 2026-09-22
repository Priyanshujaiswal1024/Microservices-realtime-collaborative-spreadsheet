package com.collab.spreadsheet.collab.websocket;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

@Component
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final SecretKey secretKey;

    public WebSocketAuthChannelInterceptor(@Value("${jwt.secret:your-256-bit-secret-change-this-in-production-please-use-env-variable}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (!StringUtils.hasText(token)) {
                token = accessor.getFirstNativeHeader("token");
            }
            if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
                token = token.substring(7).trim();
            }

            if (StringUtils.hasText(token)) {
                // If it looks like a 3-part JWT, verify it
                if (token.contains(".") && token.split("\\.").length >= 3) {
                    try {
                        Claims claims = Jwts.parser()
                                .verifyWith(secretKey)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();

                        String userId = claims.getSubject();
                        Principal principal = () -> userId;
                        accessor.setUser(principal);
                        log.debug("WebSocket client authenticated with JWT: userId={}", userId);
                    } catch (Exception e) {
                        log.debug("JWT verification skipped: {}", e.getMessage());
                    }
                } else {
                    // Dev / mock user ID token
                    final String devUserId = token;
                    accessor.setUser(() -> devUserId);
                    log.debug("WebSocket client attached with user ID: {}", devUserId);
                }
            } else {
                log.debug("WebSocket CONNECT without explicit token (guest/anonymous session)");
            }
        }

        return message;
    }
}
