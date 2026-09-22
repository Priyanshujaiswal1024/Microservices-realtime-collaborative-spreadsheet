package com.collab.spreadsheet.sheet.controller;

import com.collab.spreadsheet.sheet.dto.ProtectedRangeDto;
import com.collab.spreadsheet.sheet.dto.ProtectedRangeRequest;
import com.collab.spreadsheet.sheet.service.ProtectedRangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sheets/{sheetId}/protected-ranges")
@RequiredArgsConstructor
@Slf4j
public class ProtectedRangeController {

    private final ProtectedRangeService protectedRangeService;

    @GetMapping
    public ResponseEntity<List<ProtectedRangeDto>> getProtectedRanges(
            @PathVariable String sheetId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(protectedRangeService.getProtectedRanges(sheetId, userId));
    }

    @PostMapping
    public ResponseEntity<ProtectedRangeDto> createProtectedRange(
            @PathVariable String sheetId,
            @Valid @RequestBody ProtectedRangeRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("REST request to protect range in sheet {} by user {}", sheetId, userId);
        ProtectedRangeDto range = protectedRangeService.createProtectedRange(sheetId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(range);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProtectedRange(
            @PathVariable String sheetId,
            @PathVariable String id,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        protectedRangeService.deleteProtectedRange(id, userId);
        return ResponseEntity.ok(Map.of("message", "Protected range removed successfully"));
    }
}
