package com.collab.spreadsheet.collab.websocket;

import com.collab.spreadsheet.collab.service.PresenceTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final PresenceTracker presenceTracker;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        java.util.Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        
        String userId = null;
        String sheetId = null;
        String userName = "Collaborator";

        if (sessionAttributes != null) {
            sheetId = (String) sessionAttributes.get("sheetId");
            userId = (String) sessionAttributes.get("userId");
            if (sessionAttributes.get("userName") != null) {
                userName = (String) sessionAttributes.get("userName");
            }
        }

        if (userId == null && headerAccessor.getUser() != null) {
            userId = headerAccessor.getUser().getName();
        }

        if (sheetId != null && userId != null) {
            log.info("Cleaning up presence on WebSocket disconnect: user={}, sheet={}", userId, sheetId);
            presenceTracker.handleLeave(sheetId, userId, userName);
        } else if (userId != null) {
            log.info("User disconnected from WebSocket without mapped sheet: {}", userId);
        }
    }
}
