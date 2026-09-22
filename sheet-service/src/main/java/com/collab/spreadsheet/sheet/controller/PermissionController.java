package com.collab.spreadsheet.sheet.controller;

import com.collab.spreadsheet.sheet.dto.PermissionDto;
import com.collab.spreadsheet.sheet.dto.ShareWorkbookRequest;
import com.collab.spreadsheet.sheet.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workbooks/{workbookId}/permissions")
@RequiredArgsConstructor
@Slf4j
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<PermissionDto>> getPermissions(
            @PathVariable String workbookId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(permissionService.getWorkbookPermissions(workbookId, userId));
    }

    @PostMapping
    public ResponseEntity<PermissionDto> shareWorkbook(
            @PathVariable String workbookId,
            @Valid @RequestBody ShareWorkbookRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("REST request to share workbook {} with {} as {}", workbookId, request.getUserId(), request.getRole());
        PermissionDto permission = permissionService.shareWorkbook(workbookId, request, userId);
        return ResponseEntity.ok(permission);
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Map<String, String>> revokePermission(
            @PathVariable String workbookId,
            @PathVariable String targetUserId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        permissionService.removePermission(workbookId, targetUserId, userId);
        return ResponseEntity.ok(Map.of("message", "Permission removed successfully"));
    }
}
