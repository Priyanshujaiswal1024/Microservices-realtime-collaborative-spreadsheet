package com.collab.spreadsheet.collab.redis;

import com.collab.spreadsheet.common.websocket.CellBatchUpdateMessage;
import com.collab.spreadsheet.common.websocket.CellUpdateMessage;
import com.collab.spreadsheet.common.websocket.CursorMoveMessage;
import com.collab.spreadsheet.common.websocket.PresenceMessage;
import com.collab.spreadsheet.common.websocket.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            // Channel format: "sheet:{sheetId}:broadcast"
            String[] parts = channel.split(":");
            if (parts.length >= 2) {
                String sheetId = parts[1];
                WebSocketMessage wsMsg = objectMapper.readValue(body, WebSocketMessage.class);

                if (wsMsg instanceof CellUpdateMessage || wsMsg instanceof CellBatchUpdateMessage) {
                    messagingTemplate.convertAndSend("/topic/sheet/" + sheetId + "/cells", wsMsg);
                } else if (wsMsg instanceof CursorMoveMessage || wsMsg instanceof PresenceMessage) {
                    messagingTemplate.convertAndSend("/topic/sheet/" + sheetId + "/presence", wsMsg);
                } else {
                    messagingTemplate.convertAndSend("/topic/sheet/" + sheetId + "/updates", wsMsg);
                }
            }
        } catch (Exception e) {
            log.error("Error processing Redis pub/sub message: {}", e.getMessage());
        }
    }
}
