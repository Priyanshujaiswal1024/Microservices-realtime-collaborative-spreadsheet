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

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Transactional(readOnly = true)
    public byte[] exportToXlsx(String workbookId, String userId) throws IOException {
        Workbook workbook = workbookRepository.findById(workbookId)
                .orElseThrow(() -> new ResourceNotFoundException("Workbook", workbookId));

        permissionService.requireViewAccess(workbook, userId);

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook poiWorkbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Map<String, CellStyle> styleCache = new HashMap<>();
            List<Sheet> sheets = sheetRepository.findByWorkbookIdOrderByPositionAsc(workbookId);

            for (Sheet sheetEntity : sheets) {
                org.apache.poi.xssf.usermodel.XSSFSheet poiSheet = poiWorkbook.createSheet(sheetEntity.getName());
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

                    // Apply format from cell format JSON (fill color, text color, bold, alignment)
                    if (cellEntity.getFormat() != null && !cellEntity.getFormat().isBlank()) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> formatMap = objectMapper.readValue(cellEntity.getFormat(), Map.class);
                            applyFormatToCell(poiWorkbook, poiCell, styleCache, formatMap);
                        } catch (Exception e) {
                            log.debug("Failed to parse format JSON for cell ({},{}): {}", r, c, cellEntity.getFormat());
                        }
                    }
                }
            }

            poiWorkbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportLiveToXlsx(com.collab.spreadsheet.sheet.dto.LiveWorkbookExportRequest request) throws IOException {
        if (request == null || request.getSheets() == null || request.getSheets().isEmpty()) {
            try (org.apache.poi.xssf.usermodel.XSSFWorkbook emptyWb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                emptyWb.createSheet("Sheet1");
                emptyWb.write(out);
                return out.toByteArray();
            }
        }

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook poiWorkbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Map<String, CellStyle> styleCache = new HashMap<>();

            for (com.collab.spreadsheet.sheet.dto.LiveWorkbookExportRequest.LiveSheetExportDto sheetDto : request.getSheets()) {
                String sheetName = sheetDto.getName() != null && !sheetDto.getName().isBlank()
                        ? sheetDto.getName().replaceAll("[\\\\/?*\\[\\]]", "")
                        : "Sheet";
                org.apache.poi.xssf.usermodel.XSSFSheet poiSheet = poiWorkbook.createSheet(sheetName);

                if (sheetDto.getCells() == null) continue;

                for (com.collab.spreadsheet.sheet.dto.LiveWorkbookExportRequest.LiveCellExportDto cellDto : sheetDto.getCells()) {
                    if (cellDto.getRow() == null || cellDto.getCol() == null) continue;
                    int r = cellDto.getRow();
                    int c = cellDto.getCol();

                    org.apache.poi.ss.usermodel.Row poiRow = poiSheet.getRow(r);
                    if (poiRow == null) {
                        poiRow = poiSheet.createRow(r);
                    }

                    org.apache.poi.ss.usermodel.Cell poiCell = poiRow.createCell(c);
                    String val = cellDto.getValue();

                    if ("NUMBER".equalsIgnoreCase(cellDto.getDataType())) {
                        try {
                            poiCell.setCellValue(Double.parseDouble(val));
                        } catch (Exception e) {
                            poiCell.setCellValue(val != null ? val : "");
                        }
                    } else if ("BOOLEAN".equalsIgnoreCase(cellDto.getDataType())) {
                        poiCell.setCellValue(Boolean.parseBoolean(val));
                    } else {
                        poiCell.setCellValue(val != null ? val : "");
                    }

                    // Apply fill background colors, text colors, bold, etc.
                    applyFormatToCell(poiWorkbook, poiCell, styleCache, cellDto.getFormat());
                }
            }

            poiWorkbook.write(out);
            return out.toByteArray();
        }
    }

    private void applyFormatToCell(org.apache.poi.xssf.usermodel.XSSFWorkbook workbook,
                                   org.apache.poi.ss.usermodel.Cell cell,
                                   Map<String, CellStyle> styleCache,
                                   Map<String, Object> format) {
        if (format == null || format.isEmpty()) return;

        String bgColor = format.get("backgroundColor") != null ? String.valueOf(format.get("backgroundColor")) : null;
        String textColor = format.get("textColor") != null ? String.valueOf(format.get("textColor")) : null;
        boolean bold = Boolean.TRUE.equals(format.get("bold")) || "true".equalsIgnoreCase(String.valueOf(format.get("bold")));
        boolean italic = Boolean.TRUE.equals(format.get("italic")) || "true".equalsIgnoreCase(String.valueOf(format.get("italic")));
        boolean strike = Boolean.TRUE.equals(format.get("strike")) || "true".equalsIgnoreCase(String.valueOf(format.get("strike")));
        String align = format.get("align") != null ? String.valueOf(format.get("align")) : null;

        if (bgColor == null && textColor == null && !bold && !italic && !strike && align == null) {
            return;
        }

        String cacheKey = String.format("%s|%s|%b|%b|%b|%s", bgColor, textColor, bold, italic, strike, align);
        CellStyle style = styleCache.computeIfAbsent(cacheKey, k -> {
            org.apache.poi.xssf.usermodel.XSSFCellStyle s = workbook.createCellStyle();

            if (bgColor != null && !bgColor.isBlank()) {
                byte[] rgb = parseColor(bgColor);
                if (rgb != null) {
                    s.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(rgb, null));
                    s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
            }

            org.apache.poi.xssf.usermodel.XSSFFont font = workbook.createFont();
            boolean fontChanged = false;
            if (bold) {
                font.setBold(true);
                fontChanged = true;
            }
            if (italic) {
                font.setItalic(true);
                fontChanged = true;
            }
            if (strike) {
                font.setStrikeout(true);
                fontChanged = true;
            }
            if (textColor != null && !textColor.isBlank()) {
                byte[] rgb = parseColor(textColor);
                if (rgb != null) {
                    font.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(rgb, null));
                    fontChanged = true;
                }
            }
            if (fontChanged) {
                s.setFont(font);
            }

            if ("center".equalsIgnoreCase(align)) {
                s.setAlignment(HorizontalAlignment.CENTER);
            } else if ("right".equalsIgnoreCase(align)) {
                s.setAlignment(HorizontalAlignment.RIGHT);
            } else if ("left".equalsIgnoreCase(align)) {
                s.setAlignment(HorizontalAlignment.LEFT);
            }

            return s;
        });

        cell.setCellStyle(style);
    }

    private byte[] parseColor(String str) {
        if (str == null || str.isBlank()) return null;
        str = str.trim().toLowerCase();
        try {
            if (str.startsWith("#")) {
                String hex = str.substring(1);
                if (hex.length() == 3) {
                    char r = hex.charAt(0);
                    char g = hex.charAt(1);
                    char b = hex.charAt(2);
                    hex = "" + r + r + g + g + b + b;
                }
                if (hex.length() == 6) {
                    int r = Integer.parseInt(hex.substring(0, 2), 16);
                    int g = Integer.parseInt(hex.substring(2, 4), 16);
                    int b = Integer.parseInt(hex.substring(4, 6), 16);
                    return new byte[]{(byte) r, (byte) g, (byte) b};
                }
            } else if (str.startsWith("rgb")) {
                String inside = str.replaceAll("[^0-9,]", "");
                String[] parts = inside.split(",");
                if (parts.length >= 3) {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    return new byte[]{(byte) r, (byte) g, (byte) b};
                }
            } else {
                switch (str) {
                    case "red": return new byte[]{(byte) 234, 67, 53};
                    case "green": return new byte[]{52, (byte) 168, 83};
                    case "yellow": return new byte[]{(byte) 251, (byte) 188, 4};
                    case "blue": return new byte[]{66, (byte) 133, (byte) 244};
                    case "orange": return new byte[]{(byte) 245, (byte) 158, 11};
                    case "purple": return new byte[]{(byte) 168, 85, (byte) 247};
                    case "white": return new byte[]{(byte) 255, (byte) 255, (byte) 255};
                    case "black": return new byte[]{0, 0, 0};
                    default: break;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse color '{}': {}", str, e.getMessage());
        }
        return null;
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
