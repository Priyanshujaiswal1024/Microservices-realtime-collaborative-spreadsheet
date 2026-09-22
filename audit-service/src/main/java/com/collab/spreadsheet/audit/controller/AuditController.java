package com.collab.spreadsheet.audit.controller;

import com.collab.spreadsheet.audit.dto.*;
import com.collab.spreadsheet.audit.service.AuditService;
import com.collab.spreadsheet.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/workbooks/{workbookId}/history")
    public ResponseEntity<PageResponse<AuditEventDto>> getWorkbookHistory(
            @PathVariable String workbookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditService.getWorkbookHistory(workbookId, pageable));
    }

    @GetMapping("/sheets/{sheetId}/history")
    public ResponseEntity<PageResponse<AuditEventDto>> getSheetHistory(
            @PathVariable String sheetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditService.getSheetHistory(sheetId, pageable));
    }

    @GetMapping("/workbooks/{workbookId}/snapshots")
    public ResponseEntity<List<SnapshotDto>> getSnapshots(@PathVariable String workbookId) {
        return ResponseEntity.ok(auditService.getWorkbookSnapshots(workbookId));
    }

    @PostMapping("/workbooks/{workbookId}/snapshots")
    public ResponseEntity<SnapshotDto> createSnapshot(
            @PathVariable String workbookId,
            @Valid @RequestBody CreateSnapshotRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        SnapshotDto snapshot = auditService.createSnapshot(workbookId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(snapshot);
    }

    @PostMapping("/workbooks/{workbookId}/restore")
    public ResponseEntity<RestoreResponse> restoreWorkbook(
            @PathVariable String workbookId,
            @RequestBody RestoreSnapshotRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        RestoreResponse response = auditService.restoreWorkbook(workbookId, request, userId);
        return ResponseEntity.ok(response);
    }
}
