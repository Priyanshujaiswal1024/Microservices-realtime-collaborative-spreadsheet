package com.collab.spreadsheet.collab.controller;

import com.collab.spreadsheet.collab.redis.RedisCellStateRepository;
import com.collab.spreadsheet.collab.redis.RedisStreamReplayBuffer;
import com.collab.spreadsheet.collab.service.PresenceTracker;
import com.collab.spreadsheet.common.crdt.CellState;
import com.collab.spreadsheet.common.websocket.CellUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/collab/sheet/{sheetId}")
@RequiredArgsConstructor
@Slf4j
public class CollabRestController {

    private final RedisCellStateRepository cellStateRepository;
    private final RedisStreamReplayBuffer replayBuffer;
    private final PresenceTracker presenceTracker;

    @GetMapping("/state")
    public ResponseEntity<Map<String, CellState>> getSheetState(@PathVariable String sheetId) {
        log.info("REST request to fetch live CRDT state for sheet: {}", sheetId);
        Map<String, CellState> state = cellStateRepository.getAllCells(sheetId);
        return ResponseEntity.ok(state);
    }

    @GetMapping("/replay")
    public ResponseEntity<List<CellUpdateMessage>> getReplayStream(
            @PathVariable String sheetId,
            @RequestParam(required = false, defaultValue = "-") String lastOpId,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        log.info("REST request to replay stream for sheet {} from opId {}", sheetId, lastOpId);
        List<CellUpdateMessage> ops = replayBuffer.getReplayOps(sheetId, lastOpId, limit);
        return ResponseEntity.ok(ops);
    }

    @GetMapping("/presence")
    public ResponseEntity<List<PresenceTracker.UserPresenceInfo>> getActivePresence(@PathVariable String sheetId) {
        List<PresenceTracker.UserPresenceInfo> active = presenceTracker.getActiveUsers(sheetId);
        return ResponseEntity.ok(active);
    }
}
