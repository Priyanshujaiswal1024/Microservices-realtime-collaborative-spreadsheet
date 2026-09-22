package com.collab.spreadsheet.sheet.controller;

import com.collab.spreadsheet.sheet.dto.WorkbookDto;
import com.collab.spreadsheet.sheet.service.ExcelExportImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ExportImportController {

    private final ExcelExportImportService exportImportService;

    @GetMapping("/workbooks/{workbookId}/export/xlsx")
    public ResponseEntity<byte[]> exportWorkbookToXlsx(
            @PathVariable String workbookId,
            Authentication authentication) throws IOException {
        String userId = (String) authentication.getPrincipal();
        log.info("REST request to export workbook {} to XLSX by {}", workbookId, userId);
        byte[] bytes = exportImportService.exportToXlsx(workbookId, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "workbook-" + workbookId + ".xlsx");
        headers.setContentLength(bytes.length);

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/sheets/{sheetId}/export/csv")
    public ResponseEntity<byte[]> exportSheetToCsv(
            @PathVariable String sheetId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("REST request to export sheet {} to CSV by {}", sheetId, userId);
        byte[] bytes = exportImportService.exportToCsv(sheetId, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "sheet-" + sheetId + ".csv");
        headers.setContentLength(bytes.length);

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @PostMapping("/workbooks/import")
    public ResponseEntity<WorkbookDto> importWorkbook(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        String userId = (String) authentication.getPrincipal();
        log.info("REST request to import file {} by {}", file.getOriginalFilename(), userId);
        WorkbookDto dto = exportImportService.importWorkbook(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
