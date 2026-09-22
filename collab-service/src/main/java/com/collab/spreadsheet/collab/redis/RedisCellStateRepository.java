package com.collab.spreadsheet.collab.redis;

import com.collab.spreadsheet.common.crdt.CellState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisCellStateRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "sheet:";
    private static final String KEY_SUFFIX = ":cells";

    private String getHashKey(String sheetId) {
        return KEY_PREFIX + sheetId + KEY_SUFFIX;
    }

    private String getCellField(int row, int col) {
        return row + ":" + col;
    }

    public CellState getCell(String sheetId, int row, int col) {
        String json = (String) redisTemplate.opsForHash().get(getHashKey(sheetId), getCellField(row, col));
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, CellState.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize CellState for sheet {}, cell ({},{}): {}", sheetId, row, col, e.getMessage());
            return null;
        }
    }

    public Map<String, CellState> getCells(String sheetId, List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<Object> rawList = redisTemplate.opsForHash().multiGet(getHashKey(sheetId), new ArrayList<>(fields));
        Map<String, CellState> result = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            Object raw = rawList.get(i);
            if (raw instanceof String json && !json.isBlank()) {
                try {
                    result.put(fields.get(i), objectMapper.readValue(json, CellState.class));
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize CellState for field {}: {}", fields.get(i), e.getMessage());
                }
            }
        }
        return result;
    }

    public void saveCell(String sheetId, int row, int col, CellState cellState) {
        try {
            String json = objectMapper.writeValueAsString(cellState);
            redisTemplate.opsForHash().put(getHashKey(sheetId), getCellField(row, col), json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize CellState for sheet {}, cell ({},{}): {}", sheetId, row, col, e.getMessage());
        }
    }

    public void saveCells(String sheetId, Map<String, CellState> cellMap) {
        if (cellMap == null || cellMap.isEmpty()) return;
        Map<String, String> jsonMap = new HashMap<>();
        for (Map.Entry<String, CellState> entry : cellMap.entrySet()) {
            try {
                jsonMap.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize CellState for key {}: {}", entry.getKey(), e.getMessage());
            }
        }
        if (!jsonMap.isEmpty()) {
            redisTemplate.opsForHash().putAll(getHashKey(sheetId), jsonMap);
        }
    }

    public Map<String, CellState> getAllCells(String sheetId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(getHashKey(sheetId));
        Map<String, CellState> result = new HashMap<>();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String field = (String) entry.getKey();
            String json = (String) entry.getValue();
            try {
                CellState state = objectMapper.readValue(json, CellState.class);
                result.put(field, state);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize CellState for field {}: {}", field, e.getMessage());
            }
        }
        return result;
    }

    public void clearSheet(String sheetId) {
        redisTemplate.delete(getHashKey(sheetId));
    }
}
