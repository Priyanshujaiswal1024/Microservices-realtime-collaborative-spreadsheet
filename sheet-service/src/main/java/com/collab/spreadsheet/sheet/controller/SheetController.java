package com.collab.spreadsheet.sheet.controller;

import com.collab.spreadsheet.sheet.dto.CreateSheetRequest;
import com.collab.spreadsheet.sheet.dto.SheetDto;
import com.collab.spreadsheet.sheet.dto.UpdateSheetRequest;
import com.collab.spreadsheet.sheet.service.SheetService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class SheetController {

    private final SheetService sheetService;

    @PostMapping("/workbooks/{workbookId}/sheets")
    public ResponseEntity<SheetDto> createSheet(
            @PathVariable String workbookId,
            @Valid @RequestBody CreateSheetRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("REST request to add sheet '{}' to workbook {} by {}", request.getName(), workbookId, userId);
        SheetDto sheet = sheetService.createSheet(workbookId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(sheet);
    }

    @GetMapping("/workbooks/{workbookId}/sheets")
    public ResponseEntity<List<SheetDto>> getSheets(
            @PathVariable String workbookId,
            Authentication authentication) {
        String userId = authentication != null ? (String) authentication.getPrincipal() : "anonymous";
        List<SheetDto> sheets = sheetService.getSheets(workbookId, userId);
        return ResponseEntity.ok(sheets);
    }

    @GetMapping("/sheets/{sheetId}")
    public ResponseEntity<SheetDto> getSheet(
            @PathVariable String sheetId,
            Authentication authentication) {
        String userId = authentication != null ? (String) authentication.getPrincipal() : "anonymous";
        SheetDto sheet = sheetService.getSheet(sheetId, userId);
        return ResponseEntity.ok(sheet);
    }

    @PutMapping("/sheets/{sheetId}")
    public ResponseEntity<SheetDto> updateSheet(
            @PathVariable String sheetId,
            @Valid @RequestBody UpdateSheetRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        SheetDto updated = sheetService.updateSheet(sheetId, request, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/sheets/{sheetId}")
    public ResponseEntity<Map<String, String>> deleteSheet(
            @PathVariable String sheetId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        sheetService.deleteSheet(sheetId, userId);
        return ResponseEntity.ok(Map.of("message", "Sheet deleted successfully"));
    }
}
