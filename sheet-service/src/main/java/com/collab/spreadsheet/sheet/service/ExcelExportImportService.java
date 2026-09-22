package com.collab.spreadsheet.sheet.service;

import com.collab.spreadsheet.common.dto.PermissionRole;
import com.collab.spreadsheet.common.dto.WorkbookVisibility;
import com.collab.spreadsheet.common.exception.ResourceNotFoundException;
import com.collab.spreadsheet.sheet.dto.WorkbookDto;
import com.collab.spreadsheet.sheet.entity.Cell;
import com.collab.spreadsheet.sheet.entity.Permission;
import com.collab.spreadsheet.sheet.entity.Sheet;
import com.collab.spreadsheet.sheet.entity.Workbook;
import com.collab.spreadsheet.sheet.mapper.WorkbookMapper;
import com.collab.spreadsheet.sheet.repository.CellRepository;
import com.collab.spreadsheet.sheet.repository.PermissionRepository;
import com.collab.spreadsheet.sheet.repository.SheetRepository;
import com.collab.spreadsheet.sheet.repository.WorkbookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportImportService {

    private final WorkbookRepository workbookRepository;
    private final SheetRepository sheetRepository;
    private final CellRepository cellRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionService permissionService;
    private final WorkbookMapper workbookMapper;

    @Transactional(readOnly = true)
    public byte[] exportToXlsx(String workbookId, String userId) throws IOException {
        Workbook workbook = workbookRepository.findById(workbookId)
                .orElseThrow(() -> new ResourceNotFoundException("Workbook", workbookId));

        permissionService.requireViewAccess(workbook, userId);

        try (org.apache.poi.ss.usermodel.Workbook poiWorkbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            List<Sheet> sheets = sheetRepository.findByWorkbookIdOrderByPositionAsc(workbookId);

            for (Sheet sheetEntity : sheets) {
                org.apache.poi.ss.usermodel.Sheet poiSheet = poiWorkbook.createSheet(sheetEntity.getName());
                List<Cell> cells = cellRepository.findBySheetId(sheetEntity.getId());

                for (Cell cellEntity : cells) {
                    if (cellEntity.getValue() == null) continue;

                    int r = cellEntity.getRow();
                    int c = cellEntity.getCol();

                    org.apache.poi.ss.usermodel.Row poiRow = poiSheet.getRow(r);
                    if (poiRow == null) {
                        poiRow = poiSheet.createRow(r);
                    }

                    org.apache.poi.ss.usermodel.Cell poiCell = poiRow.createCell(c);
                    String val = cellEntity.getValue();

                    if ("NUMBER".equalsIgnoreCase(cellEntity.getDataType())) {
                        try {
                            poiCell.setCellValue(Double.parseDouble(val));
                        } catch (NumberFormatException e) {
                            poiCell.setCellValue(val);
                        }
                    } else if ("BOOLEAN".equalsIgnoreCase(cellEntity.getDataType())) {
                        poiCell.setCellValue(Boolean.parseBoolean(val));
                    } else {
                        poiCell.setCellValue(val);
                    }
                }
            }

            poiWorkbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportToCsv(String sheetId, String userId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet", sheetId));

        permissionService.requireViewAccess(sheet.getWorkbook(), userId);

        List<Cell> cells = cellRepository.findBySheetId(sheetId);
        Map<Integer, Map<Integer, String>> grid = new HashMap<>();
        int maxRow = 0;
        int maxCol = 0;

        for (Cell cell : cells) {
            grid.computeIfAbsent(cell.getRow(), k -> new HashMap<>()).put(cell.getCol(), cell.getValue());
            maxRow = Math.max(maxRow, cell.getRow());
            maxCol = Math.max(maxCol, cell.getCol());
        }

        StringBuilder sb = new StringBuilder();
        for (int r = 0; r <= maxRow; r++) {
            Map<Integer, String> rowMap = grid.getOrDefault(r, Collections.emptyMap());
            for (int c = 0; c <= maxCol; c++) {
                String val = rowMap.getOrDefault(c, "");
                sb.append(escapeCsvField(val));
                if (c < maxCol) sb.append(",");
            }
            sb.append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public WorkbookDto importWorkbook(MultipartFile file, String userId) throws IOException {
        String filename = file.getOriginalFilename();
        String title = (filename != null && filename.contains("."))
                ? filename.substring(0, filename.lastIndexOf('.'))
                : "Imported Workbook";

        Workbook workbook = Workbook.builder()
                .title(title)
                .ownerId(userId)
                .visibility(WorkbookVisibility.PRIVATE)
                .sheets(new ArrayList<>())
                .permissions(new ArrayList<>())
                .build();
        workbook = workbookRepository.save(workbook);

        Permission ownerPerm = Permission.builder()
                .workbook(workbook)
                .userId(userId)
                .role(PermissionRole.OWNER)
                .build();
        permissionRepository.save(ownerPerm);
        workbook.getPermissions().add(ownerPerm);

        if (filename != null && (filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            importXlsx(file.getInputStream(), workbook, userId);
        } else {
            importCsv(file.getInputStream(), workbook, userId);
        }

        WorkbookDto dto = workbookMapper.toDto(workbook);
        dto.setUserRole(PermissionRole.OWNER);
        return dto;
    }

    private void importXlsx(InputStream is, Workbook workbook, String userId) throws IOException {
        try (org.apache.poi.ss.usermodel.Workbook poiWorkbook = WorkbookFactory.create(is)) {
            int numSheets = poiWorkbook.getNumberOfSheets();
            for (int s = 0; s < numSheets; s++) {
                org.apache.poi.ss.usermodel.Sheet poiSheet = poiWorkbook.getSheetAt(s);
                Sheet sheet = Sheet.builder()
                        .workbook(workbook)
                        .name(poiSheet.getSheetName())
                        .position(s)
                        .rowCount(Math.max(100, poiSheet.getLastRowNum() + 1))
                        .colCount(26)
                        .build();
                sheet = sheetRepository.save(sheet);
                workbook.getSheets().add(sheet);

                List<Cell> cellsToSave = new ArrayList<>();
                for (org.apache.poi.ss.usermodel.Row row : poiSheet) {
                    for (org.apache.poi.ss.usermodel.Cell poiCell : row) {
                        String cellVal = getCellValueAsString(poiCell);
                        if (cellVal != null && !cellVal.isEmpty()) {
                            cellsToSave.add(Cell.builder()
                                    .sheet(sheet)
                                    .row(poiCell.getRowIndex())
                                    .col(poiCell.getColumnIndex())
                                    .value(cellVal)
                                    .dataType(determineDataType(poiCell))
                                    .lastModifiedBy(userId)
                                    .version(1L)
                                    .build());
                        }
                    }
                }
                if (!cellsToSave.isEmpty()) {
                    cellRepository.saveAll(cellsToSave);
                }
            }
        }
    }

    private void importCsv(InputStream is, Workbook workbook, String userId) throws IOException {
        Sheet sheet = Sheet.builder()
                .workbook(workbook)
                .name("Sheet1")
                .position(0)
                .rowCount(100)
                .colCount(26)
                .build();
        sheet = sheetRepository.save(sheet);
        workbook.getSheets().add(sheet);

        List<Cell> cellsToSave = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            int rowIdx = 0;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (int colIdx = 0; colIdx < values.length; colIdx++) {
                    String val = values[colIdx].trim();
                    if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                        val = val.substring(1, val.length() - 1).replace("\"\"", "\"");
                    }
                    if (!val.isEmpty()) {
                        cellsToSave.add(Cell.builder()
                                .sheet(sheet)
                                .row(rowIdx)
                                .col(colIdx)
                                .value(val)
                                .dataType("TEXT")
                                .lastModifiedBy(userId)
                                .version(1L)
                                .build());
                    }
                }
                rowIdx++;
            }
        }
        if (!cellsToSave.isEmpty()) {
            cellRepository.saveAll(cellsToSave);
        }
    }

    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toInstant().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return "=" + cell.getCellFormula();
            default:
                return null;
        }
    }

    private String determineDataType(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "TEXT";
        switch (cell.getCellType()) {
            case NUMERIC:
                return "NUMBER";
            case BOOLEAN:
                return "BOOLEAN";
            default:
                return "TEXT";
        }
    }

    private String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
