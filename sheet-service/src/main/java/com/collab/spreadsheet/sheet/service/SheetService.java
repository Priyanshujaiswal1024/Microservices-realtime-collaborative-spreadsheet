package com.collab.spreadsheet.sheet.service;

import com.collab.spreadsheet.common.events.SheetLifecycleEvent;
import com.collab.spreadsheet.common.exception.ResourceNotFoundException;
import com.collab.spreadsheet.sheet.dto.CreateSheetRequest;
import com.collab.spreadsheet.sheet.dto.SheetDto;
import com.collab.spreadsheet.sheet.dto.UpdateSheetRequest;
import com.collab.spreadsheet.sheet.entity.Sheet;
import com.collab.spreadsheet.sheet.entity.Workbook;
import com.collab.spreadsheet.sheet.kafka.KafkaSheetLifecycleProducer;
import com.collab.spreadsheet.sheet.mapper.SheetMapper;
import com.collab.spreadsheet.sheet.repository.SheetRepository;
import com.collab.spreadsheet.sheet.repository.WorkbookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SheetService {

    private final SheetRepository sheetRepository;
    private final WorkbookRepository workbookRepository;
    private final PermissionService permissionService;
    private final SheetMapper sheetMapper;
    private final KafkaSheetLifecycleProducer lifecycleProducer;

    @Transactional
    public SheetDto createSheet(String workbookId, CreateSheetRequest request, String userId) {
        Workbook workbook = workbookRepository.findById(workbookId)
                .orElseThrow(() -> new ResourceNotFoundException("Workbook", workbookId));

        permissionService.requireEditAccess(workbook, userId);

        int currentCount = sheetRepository.countByWorkbookId(workbookId);

        Sheet sheet = Sheet.builder()
                .workbook(workbook)
                .name(request.getName())
                .position(currentCount)
                .rowCount(request.getRowCount() != null ? request.getRowCount() : 100)
                .colCount(request.getColCount() != null ? request.getColCount() : 26)
                .build();

        sheet = sheetRepository.save(sheet);
        log.info("Created sheet '{}' (position {}) in workbook {}", sheet.getName(), sheet.getPosition(), workbookId);

        final SheetLifecycleEvent event = SheetLifecycleEvent.builder()
                .actorId(userId)
                .action(SheetLifecycleEvent.Action.SHEET_CREATED)
                .workbookId(workbookId)
                .sheetId(sheet.getId())
                .name(sheet.getName())
                .build();

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            lifecycleProducer.publishEvent(event);
                        } catch (Exception e) {
                            log.warn("Kafka publish failed after sheet create commit (non-critical): {}", e.getMessage());
                        }
                    }
                }
            );
        } else {
            try { lifecycleProducer.publishEvent(event); } catch (Exception e) {
                log.warn("Kafka publish failed (non-critical): {}", e.getMessage());
            }
        }

        return sheetMapper.toDto(sheet);
    }

    @Transactional(readOnly = true)
    public List<SheetDto> getSheets(String workbookId, String userId) {
        Workbook workbook = workbookRepository.findById(workbookId)
                .orElseThrow(() -> new ResourceNotFoundException("Workbook", workbookId));

        permissionService.requireViewAccess(workbook, userId);

        return sheetRepository.findByWorkbookIdOrderByPositionAsc(workbookId)
                .stream()
                .map(sheetMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SheetDto getSheet(String sheetId, String userId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet", sheetId));

        permissionService.requireViewAccess(sheet.getWorkbook(), userId);

        return sheetMapper.toDto(sheet);
    }

    @Transactional
    public SheetDto updateSheet(String sheetId, UpdateSheetRequest request, String userId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet", sheetId));

        permissionService.requireEditAccess(sheet.getWorkbook(), userId);

        if (request.getName() != null && !request.getName().isBlank()) {
            sheet.setName(request.getName().trim());
        }
        if (request.getPosition() != null) {
            sheet.setPosition(request.getPosition());
        }
        if (request.getRowCount() != null && request.getRowCount() > 0) {
            sheet.setRowCount(request.getRowCount());
        }
        if (request.getColCount() != null && request.getColCount() > 0) {
            sheet.setColCount(request.getColCount());
        }

        sheet = sheetRepository.save(sheet);

        lifecycleProducer.publishEvent(SheetLifecycleEvent.builder()
                .actorId(userId)
                .action(SheetLifecycleEvent.Action.SHEET_UPDATED)
                .workbookId(sheet.getWorkbook().getId())
                .sheetId(sheet.getId())
                .name(sheet.getName())
                .build());

        return sheetMapper.toDto(sheet);
    }

    @Transactional
    public void deleteSheet(String sheetId, String userId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet", sheetId));

        String workbookId = sheet.getWorkbook().getId();
        permissionService.requireEditAccess(sheet.getWorkbook(), userId);

        int totalSheets = sheetRepository.countByWorkbookId(workbookId);
        if (totalSheets <= 1) {
            throw new IllegalArgumentException("Cannot delete the only sheet in a workbook");
        }

        String name = sheet.getName();
        sheetRepository.delete(sheet);

        // Reorder remaining sheets
        List<Sheet> remaining = sheetRepository.findByWorkbookIdOrderByPositionAsc(workbookId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i);
        }
        sheetRepository.saveAll(remaining);

        log.info("Deleted sheet {} from workbook {}", sheetId, workbookId);

        final SheetLifecycleEvent deleteEvent = SheetLifecycleEvent.builder()
                .actorId(userId)
                .action(SheetLifecycleEvent.Action.SHEET_DELETED)
                .workbookId(workbookId)
                .sheetId(sheetId)
                .name(name)
                .build();

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            lifecycleProducer.publishEvent(deleteEvent);
                        } catch (Exception e) {
                            log.warn("Kafka publish failed after sheet delete commit (non-critical): {}", e.getMessage());
                        }
                    }
                }
            );
        } else {
            try { lifecycleProducer.publishEvent(deleteEvent); } catch (Exception e) {
                log.warn("Kafka publish failed (non-critical): {}", e.getMessage());
            }
        }
    }
}
