package com.collab.spreadsheet.collab.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * Validates range protection in real-time at the CRDT merge layer before state mutations.
 * Reads cached range protection policies directly from Redis for zero-latency enforcement.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProtectedRangeRedisValidator {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PROTECTED_RANGE_KEY_PREFIX = "sheet:protected-ranges:";
    private static final String PERMISSION_KEY_PREFIX = "sheet:permissions:";

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RangeRule implements Serializable {
        private String id;
        private int startRow;
        private int startCol;
        private int endRow;
        private int endCol;
        private List<String> allowedUserIds;
    }

    public List<RangeRule> getRules(String sheetId) {
        try {
            String redisKey = PROTECTED_RANGE_KEY_PREFIX + sheetId;
            String rangesJson = redisTemplate.opsForValue().get(redisKey);
            if (rangesJson == null || rangesJson.isBlank()) {
                return java.util.Collections.emptyList();
            }
            List<RangeRule> rules = objectMapper.readValue(rangesJson, new TypeReference<List<RangeRule>>() {});
            return rules != null ? rules : java.util.Collections.emptyList();
        } catch (Exception e) {
            log.error("Error reading protected rules for sheet {}: {}", sheetId, e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public boolean isCellProtected(List<RangeRule> rules, int row, int col, String userId) {
        if (rules == null || rules.isEmpty()) {
            return false;
        }
        for (RangeRule rule : rules) {
            if (row >= rule.getStartRow() && row <= rule.getEndRow() &&
                col >= rule.getStartCol() && col <= rule.getEndCol()) {

                if (userId == null || "anonymous".equals(userId) || 
                    rule.getAllowedUserIds() == null || !rule.getAllowedUserIds().contains(userId)) {
                    return true; // Cell is protected and user is not in the allowed list
                }
            }
        }
        return false;
    }

    /**
     * Checks if a cell at (row, col) is protected for the specified user
     */
    public boolean isCellProtectedForUser(String sheetId, int row, int col, String userId) {
        List<RangeRule> rules = getRules(sheetId);
        if (isCellProtected(rules, row, col, userId)) {
            log.warn("Blocked unauthorized edit at ({},{}) on sheet {} by user {}", row, col, sheetId, userId);
            return true;
        }
        return false;
    }

    /**
     * Checks if a user has at least EDITOR role for the sheet
     */
    public boolean hasEditPermission(String sheetId, String userId) {
        if (userId == null) {
            return true;
        }

        try {
            String redisKey = PERMISSION_KEY_PREFIX + sheetId;
            String role = (String) redisTemplate.opsForHash().get(redisKey, userId);

            if (role == null) {
                // If not explicitly stored in cache, default to true
                return true;
            }

            return "OWNER".equalsIgnoreCase(role) || "EDITOR".equalsIgnoreCase(role);
        } catch (Exception e) {
            log.error("Error evaluating user permission in Redis for sheet {}: {}", sheetId, e.getMessage());
            return true;
        }
    }

    /**
     * Cache protected ranges into Redis
     */
    public void cacheProtectedRanges(String sheetId, List<RangeRule> rules) {
        try {
            String redisKey = PROTECTED_RANGE_KEY_PREFIX + sheetId;
            String json = objectMapper.writeValueAsString(rules);
            redisTemplate.opsForValue().set(redisKey, json);
        } catch (Exception e) {
            log.error("Failed to cache protected ranges for sheet {}: {}", sheetId, e.getMessage());
        }
    }
}
