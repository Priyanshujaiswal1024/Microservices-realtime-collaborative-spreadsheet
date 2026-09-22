package com.collab.spreadsheet.sheet.service;

import com.collab.spreadsheet.common.dto.PermissionRole;
import com.collab.spreadsheet.common.dto.WorkbookVisibility;
import com.collab.spreadsheet.common.events.SheetLifecycleEvent;
import com.collab.spreadsheet.common.exception.ResourceNotFoundException;
import com.collab.spreadsheet.sheet.dto.CreateWorkbookRequest;
import com.collab.spreadsheet.sheet.dto.UpdateWorkbookRequest;
import com.collab.spreadsheet.sheet.dto.WorkbookDto;
import com.collab.spreadsheet.sheet.entity.Permission;
import com.collab.spreadsheet.sheet.entity.Sheet;
import com.collab.spreadsheet.sheet.entity.Workbook;
import com.collab.spreadsheet.sheet.kafka.KafkaSheetLifecycleProducer;
import com.collab.spreadsheet.sheet.mapper.WorkbookMapper;
import com.collab.spreadsheet.sheet.repository.PermissionRepository;
import com.collab.spreadsheet.sheet.repository.SheetRepository;
import com.collab.spreadsheet.sheet.repository.WorkbookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkbookService {

    private final WorkbookRepository workbookRepository;
    private final SheetRepository sheetRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionService permissionService;
    private final WorkbookMapper workbookMapper;
    private final KafkaSheetLifecycleProducer lifecycleProducer;

    @Transactional
    public WorkbookDto createWorkbook(CreateWorkbookRequest request, String ownerId) {
        log.info("Creating new workbook '{}' for owner: {}", request.getTitle(), ownerId);

        Workbook workbook = Workbook.builder()
                .title(request.getTitle())
                .ownerId(ownerId)
                .visibility(request.getVisibility() != null ? request.getVisibility() : WorkbookVisibility.PRIVATE)
                .sheets(new ArrayList<>())
                .permissions(new ArrayList<>())
                .build();

        workbook = workbookRepository.save(workbook);

        // Create default initial sheet (Sheet1)
        String sheetName = (request.getInitialSheetName() != null && !request.getInitialSheetName().isBlank())
                ? request.getInitialSheetName()
                : "Sheet1";

        Sheet initialSheet = Sheet.builder()
                .workbook(workbook)
                .name(sheetName)
                .position(0)
                .rowCount(100)
                .colCount(26)
                .build();
        sheetRepository.save(initialSheet);
        workbook.getSheets().add(initialSheet);

        // Add owner permission
        Permission ownerPermission = Permission.builder()
                .workbook(workbook)
                .userId(ownerId)
                .role(PermissionRole.OWNER)
                .build();
        permissionRepository.save(ownerPermission);
        workbook.getPermissions().add(ownerPermission);

        // Publish Kafka lifecycle event AFTER transaction commits
        // This prevents Kafka timeout/failure from rolling back the DB transaction
        SheetLifecycleEvent event = SheetLifecycleEvent.builder()
                .actorId(ownerId)
                .action(SheetLifecycleEvent.Action.WORKBOOK_CREATED)
                .workbookId(workbook.getId())
                .name(workbook.getTitle())
                .build();

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        lifecycleProducer.publishEvent(event);
                    } catch (Exception e) {
                        // Kafka unavailable — log but don't fail (workbook already saved)
                        log.warn("Kafka publish failed after workbook commit (non-critical): {}", e.getMessage());
                    }
                }
            });
        } else {
            // No active transaction — fire immediately (best-effort)
            try { lifecycleProducer.publishEvent(event); } catch (Exception e) {
                log.warn("Kafka publish failed (non-critical): {}", e.getMessage());
            }
        }

        WorkbookDto dto = workbookMapper.toDto(workbook);
        dto.setUserRole(PermissionRole.OWNER);
        return dto;
    }

    @Transactional(readOnly = true)
    public WorkbookDto getWorkbook(String id, String userId) {
        Workbook workbook = workbookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workbook", id));

        permissionService.requireViewAccess(workbook, userId);

        WorkbookDto dto = workbookMapper.toDto(workbook);
        dto.setUserRole(permissionService.getUserRole(workbook, userId));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<WorkbookDto> listUserWorkbooks(String userId) {
        List<Workbook> workbooks = workbookRepository.findAllAccessibleWorkbooks(userId);
        return workbooks.stream().map(wb -> {
            WorkbookDto dto = workbookMapper.toDto(wb);
            dto.setUserRole(permissionService.getUserRole(wb, userId));
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public WorkbookDto updateWorkbook(String id, UpdateWorkbookRequest request, String userId) {
        Workbook workbook = workbookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workbook", id));

        permissionService.requireEditAccess(workbook, userId);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            workbook.setTitle(request.getTitle().trim());
        }
        if (request.getVisibility() != null) {
            workbook.setVisibility(request.getVisibility());
        }

        workbook = workbookRepository.save(workbook);

        SheetLifecycleEvent event = SheetLifecycleEvent.builder()
                .actorId(userId)
                .action(SheetLifecycleEvent.Action.WORKBOOK_UPDATED)
                .workbookId(workbook.getId())
                .name(workbook.getTitle())
                .build();

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        lifecycleProducer.publishEvent(event);
                    } catch (Exception e) {
                        log.warn("Kafka publish failed after workbook update (non-critical): {}", e.getMessage());
                    }
                }
            });
        } else {
            try { lifecycleProducer.publishEvent(event); } catch (Exception e) {
                log.warn("Kafka publish failed (non-critical): {}", e.getMessage());
            }
        }

        WorkbookDto dto = workbookMapper.toDto(workbook);
        dto.setUserRole(permissionService.getUserRole(workbook, userId));
        return dto;
    }

    @Transactional
    public void deleteWorkbook(String id, String userId) {
        Workbook workbook = workbookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workbook", id));

        permissionService.requireOwnerAccess(workbook, userId);

        String title = workbook.getTitle();
        workbookRepository.delete(workbook);
        log.info("Workbook {} deleted by user {}", id, userId);

        SheetLifecycleEvent event = SheetLifecycleEvent.builder()
                .actorId(userId)
                .action(SheetLifecycleEvent.Action.WORKBOOK_DELETED)
                .workbookId(id)
                .name(title)
                .build();

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        lifecycleProducer.publishEvent(event);
                    } catch (Exception e) {
                        log.warn("Kafka publish failed after workbook delete (non-critical): {}", e.getMessage());
                    }
                }
            });
        } else {
            try { lifecycleProducer.publishEvent(event); } catch (Exception e) {
                log.warn("Kafka publish failed (non-critical): {}", e.getMessage());
            }
        }
    }
}
