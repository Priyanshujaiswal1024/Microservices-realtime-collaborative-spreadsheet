package com.collab.spreadsheet.collab.redis;

import com.collab.spreadsheet.common.websocket.WebSocketMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisPubSubBroadcaster {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void broadcast(String sheetId, WebSocketMessage message) {
        String channel = "sheet:" + sheetId + ":broadcast";
        try {
            String payload = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel, payload);
            log.debug("Published message to Redis channel {}: type={}", channel, message.getType());
        } catch (JsonProcessingException e) {
            log.error("Failed to broadcast message to channel {}: {}", channel, e.getMessage());
        }
    }
}
