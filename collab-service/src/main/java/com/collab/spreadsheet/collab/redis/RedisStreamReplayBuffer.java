package com.collab.spreadsheet.collab.redis;

import com.collab.spreadsheet.common.websocket.CellUpdateMessage;
import com.collab.spreadsheet.common.websocket.WebSocketMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStreamReplayBuffer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String STREAM_PREFIX = "sheet:";
    private static final String STREAM_SUFFIX = ":ops";

    private String getStreamKey(String sheetId) {
        return STREAM_PREFIX + sheetId + STREAM_SUFFIX;
    }

    public String recordOp(String sheetId, WebSocketMessage message) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("payload", objectMapper.writeValueAsString(message));

            MapRecord<String, String, String> record = StreamRecords.newRecord()
                    .in(getStreamKey(sheetId))
                    .ofMap(data);

            RecordId recordId = redisTemplate.opsForStream().add(record);
            log.debug("Recorded op in stream {} with id {}", getStreamKey(sheetId), recordId);
            return recordId != null ? recordId.getValue() : null;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize CellUpdateMessage for stream: {}", e.getMessage());
            return null;
        }
    }

    public List<CellUpdateMessage> getReplayOps(String sheetId, String lastOpId, int limit) {
        List<CellUpdateMessage> result = new ArrayList<>();
        String streamKey = getStreamKey(sheetId);

        try {
            Range<String> range;
            if (lastOpId != null && !lastOpId.isBlank() && !"-".equals(lastOpId)) {
                range = Range.rightOpen(lastOpId, "+");
            } else {
                range = Range.unbounded();
            }

            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                    .range(streamKey, range);

            if (records == null) return result;

            for (MapRecord<String, Object, Object> record : records) {
                String payload = (String) record.getValue().get("payload");
                if (payload != null) {
                    CellUpdateMessage msg = objectMapper.readValue(payload, CellUpdateMessage.class);
                    result.add(msg);
                }
                if (result.size() >= limit) break;
            }
        } catch (Exception e) {
            log.error("Failed to read replay ops for sheet {}: {}", sheetId, e.getMessage());
        }

        return result;
    }
}
