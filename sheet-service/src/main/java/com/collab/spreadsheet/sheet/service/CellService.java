package com.collab.spreadsheet.sheet.service;

import com.collab.spreadsheet.common.crdt.HybridLogicalClock;
import com.collab.spreadsheet.common.exception.ForbiddenException;
import com.collab.spreadsheet.common.exception.ResourceNotFoundException;
import com.collab.spreadsheet.sheet.dto.BulkCellUpdateRequest;
import com.collab.spreadsheet.sheet.dto.CellDto;
import com.collab.spreadsheet.sheet.entity.Cell;
import com.collab.spreadsheet.sheet.entity.Sheet;
import com.collab.spreadsheet.sheet.mapper.CellMapper;
import com.collab.spreadsheet.sheet.repository.CellRepository;
import com.collab.spreadsheet.sheet.repository.SheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CellService {

    private final CellRepository cellRepository;
    private final SheetRepository sheetRepository;
    private final PermissionService permissionService;
    private final ProtectedRangeService protectedRangeService;
    private final CellMapper cellMapper;

    @Transactional(readOnly = true)
    public List<CellDto> getSheetCells(String sheetId, String userId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet", sheetId));

        permissionService.requireViewAccess(sheet.getWorkbook(), userId);

        return cellRepository.findBySheetId(sheetId)
                .stream()
                .map(cellMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CellDto> getCellsByRange(String sheetId, int startRow, int endRow, int startCol, int endCol, String userId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet", sheetId));

        permissionService.requireViewAccess(sheet.getWorkbook(), userId);

        return cellRepository.findBySheetIdAndRange(sheetId, startRow, endRow, startCol, endCol)
                .stream()
                .map(cellMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CellDto upsertCell(String sheetId, int row, int col, String value, String dataType, 
                              String format, String hlcTs, String userId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet", sheetId));

        permissionService.requireEditAccess(sheet.getWorkbook(), userId);

        if (protectedRangeService.isCellProtectedForUser(sheetId, row, col, userId)) {
            throw new ForbiddenException(String.format("Cell (%d, %d) is in a protected range", row, col));
        }

        Optional<Cell> existingOpt = cellRepository.findBySheetIdAndRowAndCol(sheetId, row, col);

        if (existingOpt.isPresent()) {
            Cell existing = existingOpt.get();

            // CRDT LWW check: only update if incoming HLC >= existing HLC
            if (shouldApplyUpdate(existing.getLastModifiedTs(), hlcTs)) {
                existing.setValue(value);
                if (dataType != null) existing.setDataType(dataType);
                if (format != null) existing.setFormat(format);
                existing.setLastModifiedTs(hlcTs);
                existing.setLastModifiedBy(userId);
                existing.setVersion(existing.getVersion() + 1);
                existing = cellRepository.save(existing);
                return cellMapper.toDto(existing);
            }
            return cellMapper.toDto(existing);
        } else {
            Cell newCell = Cell.builder()
                    .sheet(sheet)
                    .row(row)
                    .col(col)
                    .value(value)
                    .dataType(dataType != null ? dataType : "TEXT")
                    .format(format)
                    .lastModifiedTs(hlcTs)
                    .lastModifiedBy(userId)
                    .version(1L)
                    .build();

            newCell = cellRepository.save(newCell);
            return cellMapper.toDto(newCell);
        }
    }

    @Transactional
    public List<CellDto> bulkUpsertCells(String sheetId, BulkCellUpdateRequest request, String userId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet", sheetId));

        permissionService.requireEditAccess(sheet.getWorkbook(), userId);

        List<CellDto> results = new ArrayList<>();
        if (request.getUpdates() == null) {
            return results;
        }

        for (BulkCellUpdateRequest.CellUpdateItem item : request.getUpdates()) {
            if (protectedRangeService.isCellProtectedForUser(sheetId, item.getRow(), item.getCol(), userId)) {
                continue; // Skip protected cells
            }
            CellDto updated = upsertCell(sheetId, item.getRow(), item.getCol(), item.getValue(),
                    item.getDataType(), item.getFormat(), item.getLastModifiedTs(), userId);
            results.add(updated);
        }

        return results;
    }

    @Transactional
    public void clearSheetCells(String sheetId, String userId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet", sheetId));

        permissionService.requireEditAccess(sheet.getWorkbook(), userId);

        cellRepository.deleteBySheetId(sheetId);
        log.info("Successfully cleared all cells for sheet {}", sheetId);
    }

    private boolean shouldApplyUpdate(String currentTs, String incomingTs) {
        if (currentTs == null || currentTs.isBlank()) {
            return true;
        }
        if (incomingTs == null || incomingTs.isBlank()) {
            return true;
        }
        try {
            HybridLogicalClock currentHlc = HybridLogicalClock.fromString(currentTs);
            HybridLogicalClock incomingHlc = HybridLogicalClock.fromString(incomingTs);
            return incomingHlc.compareTo(currentHlc) >= 0;
        } catch (Exception e) {
            return incomingTs.compareTo(currentTs) >= 0;
        }
    }
}
