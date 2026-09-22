package com.collab.spreadsheet.sheet.controller;

import com.collab.spreadsheet.sheet.dto.CreateWorkbookRequest;
import com.collab.spreadsheet.sheet.dto.UpdateWorkbookRequest;
import com.collab.spreadsheet.sheet.dto.WorkbookDto;
import com.collab.spreadsheet.sheet.service.WorkbookService;
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
@RequestMapping("/api/v1/workbooks")
@RequiredArgsConstructor
@Slf4j
public class WorkbookController {

    private final WorkbookService workbookService;

    @PostMapping
    public ResponseEntity<WorkbookDto> createWorkbook(
            @Valid @RequestBody CreateWorkbookRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("REST request to create workbook: {} by user: {}", request.getTitle(), userId);
        WorkbookDto workbook = workbookService.createWorkbook(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(workbook);
    }

    @GetMapping
    public ResponseEntity<List<WorkbookDto>> listWorkbooks(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("REST request to list accessible workbooks for user: {}", userId);
        List<WorkbookDto> workbooks = workbookService.listUserWorkbooks(userId);
        return ResponseEntity.ok(workbooks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkbookDto> getWorkbook(
            @PathVariable String id,
            Authentication authentication) {
        String userId = authentication != null ? (String) authentication.getPrincipal() : "anonymous";
        log.info("REST request to get workbook: {} for user: {}", id, userId);
        WorkbookDto workbook = workbookService.getWorkbook(id, userId);
        return ResponseEntity.ok(workbook);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkbookDto> updateWorkbook(
            @PathVariable String id,
            @Valid @RequestBody UpdateWorkbookRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("REST request to update workbook: {} by user: {}", id, userId);
        WorkbookDto updated = workbookService.updateWorkbook(id, request, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteWorkbook(
            @PathVariable String id,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("REST request to delete workbook: {} by user: {}", id, userId);
        workbookService.deleteWorkbook(id, userId);
        return ResponseEntity.ok(Map.of("message", "Workbook deleted successfully"));
    }
}
