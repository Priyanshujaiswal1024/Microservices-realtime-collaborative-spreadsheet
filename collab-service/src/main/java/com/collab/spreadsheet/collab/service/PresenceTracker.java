package com.collab.spreadsheet.collab.service;

import com.collab.spreadsheet.collab.redis.RedisPubSubBroadcaster;
import com.collab.spreadsheet.common.websocket.CursorMoveMessage;
import com.collab.spreadsheet.common.websocket.PresenceMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceTracker {

    private final StringRedisTemplate redisTemplate;
    private final RedisPubSubBroadcaster pubSubBroadcaster;
    private final ObjectMapper objectMapper;

    private static final String PRESENCE_PREFIX = "sheet:";
    private static final String PRESENCE_SUFFIX = ":presence";
    private static final long PRESENCE_TIMEOUT_SECONDS = 30;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserPresenceInfo {
        private String userId;
        private String userName;
        private String color;
        private Integer row;
        private Integer col;
        private Long lastActiveEpoch;
    }

    private String getPresenceKey(String sheetId) {
        return PRESENCE_PREFIX + sheetId + PRESENCE_SUFFIX;
    }

    public void handleJoin(String sheetId, String userId, String userName, String color) {
        UserPresenceInfo info = UserPresenceInfo.builder()
                .userId(userId)
                .userName(userName)
                .color(color)
                .lastActiveEpoch(Instant.now().toEpochMilli())
                .build();

        savePresence(sheetId, userId, info);

        PresenceMessage msg = new PresenceMessage();
        msg.setSheetId(sheetId);
        msg.setUserId(userId);
        msg.setUserName(userName);
        msg.setColor(color);
        msg.setAction(PresenceMessage.Action.JOIN);
        msg.setTimestamp(Instant.now());

        pubSubBroadcaster.broadcast(sheetId, msg);
    }

    public void handleLeave(String sheetId, String userId, String userName) {
        redisTemplate.opsForHash().delete(getPresenceKey(sheetId), userId);

        PresenceMessage msg = new PresenceMessage();
        msg.setSheetId(sheetId);
        msg.setUserId(userId);
        msg.setUserName(userName);
        msg.setAction(PresenceMessage.Action.LEAVE);
        msg.setTimestamp(Instant.now());

        pubSubBroadcaster.broadcast(sheetId, msg);
    }

    public void updateCursor(String sheetId, CursorMoveMessage message, String userId) {
        message.setSheetId(sheetId);
        message.setUserId(userId);
        message.setTimestamp(Instant.now());

        UserPresenceInfo info = getPresence(sheetId, userId);
        if (info != null) {
            info.setRow(message.getRow());
            info.setCol(message.getCol());
            info.setLastActiveEpoch(Instant.now().toEpochMilli());
            savePresence(sheetId, userId, info);
        }

        pubSubBroadcaster.broadcast(sheetId, message);
    }

    public List<UserPresenceInfo> getActiveUsers(String sheetId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(getPresenceKey(sheetId));
        List<UserPresenceInfo> activeUsers = new ArrayList<>();
        long now = Instant.now().toEpochMilli();
        long cutoff = now - (PRESENCE_TIMEOUT_SECONDS * 1000);

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String json = (String) entry.getValue();
            try {
                UserPresenceInfo info = objectMapper.readValue(json, UserPresenceInfo.class);
                if (info.getLastActiveEpoch() != null && info.getLastActiveEpoch() >= cutoff) {
                    activeUsers.add(info);
                } else {
                    // Stale presence, clean up
                    redisTemplate.opsForHash().delete(getPresenceKey(sheetId), entry.getKey());
                }
            } catch (Exception e) {
                log.error("Failed to parse presence: {}", e.getMessage());
            }
        }
        return activeUsers;
    }

    private void savePresence(String sheetId, String userId, UserPresenceInfo info) {
        try {
            String json = objectMapper.writeValueAsString(info);
            redisTemplate.opsForHash().put(getPresenceKey(sheetId), userId, json);
            redisTemplate.expire(getPresenceKey(sheetId), PRESENCE_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to save presence: {}", e.getMessage());
        }
    }

    private UserPresenceInfo getPresence(String sheetId, String userId) {
        String json = (String) redisTemplate.opsForHash().get(getPresenceKey(sheetId), userId);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, UserPresenceInfo.class);
        } catch (Exception e) {
            return null;
        }
    }
}
