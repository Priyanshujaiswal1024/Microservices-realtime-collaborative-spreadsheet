package com.collab.spreadsheet.sheet.controller;

import com.collab.spreadsheet.sheet.dto.BulkCellUpdateRequest;
import com.collab.spreadsheet.sheet.dto.CellDto;
import com.collab.spreadsheet.sheet.service.CellService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sheets/{sheetId}/cells")
@RequiredArgsConstructor
@Slf4j
public class CellController {

    private final CellService cellService;

    @GetMapping
    public ResponseEntity<List<CellDto>> getSheetCells(
            @PathVariable String sheetId,
            @RequestParam(required = false) Integer startRow,
            @RequestParam(required = false) Integer endRow,
            @RequestParam(required = false) Integer startCol,
            @RequestParam(required = false) Integer endCol,
            Authentication authentication) {
        String userId = authentication != null ? (String) authentication.getPrincipal() : "anonymous";

        if (startRow != null && endRow != null && startCol != null && endCol != null) {
            return ResponseEntity.ok(cellService.getCellsByRange(sheetId, startRow, endRow, startCol, endCol, userId));
        }

        return ResponseEntity.ok(cellService.getSheetCells(sheetId, userId));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<CellDto>> bulkUpdateCells(
            @PathVariable String sheetId,
            @RequestBody BulkCellUpdateRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("REST request to bulk update cells in sheet {} by user {}", sheetId, userId);
        List<CellDto> updated = cellService.bulkUpsertCells(sheetId, request, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearSheetCells(
            @PathVariable String sheetId,
            Authentication authentication) {
        String userId = authentication != null ? (String) authentication.getPrincipal() : "anonymous";
        log.info("REST request to clear all cells in sheet {} by user {}", sheetId, userId);
        cellService.clearSheetCells(sheetId, userId);
        return ResponseEntity.noContent().build();
    }
}
