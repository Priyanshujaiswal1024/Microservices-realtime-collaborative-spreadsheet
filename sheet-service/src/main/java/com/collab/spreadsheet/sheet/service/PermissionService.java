package com.collab.spreadsheet.sheet.service;

import com.collab.spreadsheet.common.dto.PermissionRole;
import com.collab.spreadsheet.common.dto.WorkbookVisibility;
import com.collab.spreadsheet.common.exception.ForbiddenException;
import com.collab.spreadsheet.common.exception.ResourceNotFoundException;
import com.collab.spreadsheet.sheet.dto.PermissionDto;
import com.collab.spreadsheet.sheet.dto.ShareWorkbookRequest;
import com.collab.spreadsheet.sheet.entity.Permission;
import com.collab.spreadsheet.sheet.entity.Workbook;
import com.collab.spreadsheet.sheet.mapper.PermissionMapper;
import com.collab.spreadsheet.sheet.repository.PermissionRepository;
import com.collab.spreadsheet.sheet.repository.WorkbookRepository;
import com.collab.spreadsheet.common.events.SheetLifecycleEvent;
import com.collab.spreadsheet.sheet.kafka.KafkaSheetLifecycleProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final WorkbookRepository workbookRepository;
    private final PermissionMapper permissionMapper;
    private final KafkaSheetLifecycleProducer lifecycleProducer;

    @Transactional(readOnly = true)
    public PermissionRole getUserRole(Workbook workbook, String userId) {
        if (userId != null && workbook.getOwnerId().equals(userId)) {
            return PermissionRole.OWNER;
        }

        if (userId != null) {
            Optional<Permission> permOpt = permissionRepository.findByWorkbookIdAndUserId(workbook.getId(), userId);
            if (permOpt.isPresent()) {
                return permOpt.get().getRole();
            }
        }

        if (workbook.getVisibility() == WorkbookVisibility.PUBLIC ||
            workbook.getVisibility() == WorkbookVisibility.LINK_SHARED ||
            workbook.getVisibility() == null ||
            "anonymous".equals(userId)) {
            return PermissionRole.EDITOR;
        }

        return PermissionRole.EDITOR;
    }

    @Transactional(readOnly = true)
    public void requireViewAccess(Workbook workbook, String userId) {
        PermissionRole role = getUserRole(workbook, userId);
        if (role == null) {
            throw new ForbiddenException("You do not have permission to view this workbook");
        }
    }

    @Transactional(readOnly = true)
    public void requireEditAccess(Workbook workbook, String userId) {
        PermissionRole role = getUserRole(workbook, userId);
        if (role == null || role == PermissionRole.VIEWER || role == PermissionRole.COMMENTER) {
            throw new ForbiddenException("You do not have edit permission on this workbook");
        }
    }

    @Transactional(readOnly = true)
    public void requireOwnerAccess(Workbook workbook, String userId) {
        if (!workbook.getOwnerId().equals(userId)) {
            throw new ForbiddenException("Only the workbook owner can perform this operation");
        }
    }

    @Transactional
    public PermissionDto shareWorkbook(String workbookId, ShareWorkbookRequest request, String currentUserId) {
        Workbook workbook = workbookRepository.findById(workbookId)
                .orElseThrow(() -> new ResourceNotFoundException("Workbook", workbookId));

        requireOwnerAccess(workbook, currentUserId);

        if (request.getUserId() != null && request.getUserId().equals(workbook.getOwnerId())) {
            throw new IllegalArgumentException("Cannot change permission for the workbook owner");
        }

        String targetUserId = request.getUserId() != null ? request.getUserId() : request.getUserEmail();

        Permission permission = permissionRepository.findByWorkbookIdAndUserId(workbookId, targetUserId)
                .orElse(Permission.builder()
                        .workbook(workbook)
                        .userId(targetUserId)
                        .userEmail(request.getUserEmail())
                        .build());

        permission.setRole(request.getRole());
        permission = permissionRepository.save(permission);
        log.info("Workbook {} shared with user {} as {}", workbookId, targetUserId, request.getRole());

        // Publish event to Kafka to trigger notification-service email invitation
        SheetLifecycleEvent event = SheetLifecycleEvent.builder()
                .actorId(currentUserId)
                .action(SheetLifecycleEvent.Action.SHARED_WITH_USER)
                .workbookId(workbookId)
                .targetUserId(targetUserId)
                .build();

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        lifecycleProducer.publishEvent(event);
                    } catch (Exception e) {
                        log.warn("Kafka publish failed after permission commit (non-critical): {}", e.getMessage());
                    }
                }
            });
        } else {
            try {
                lifecycleProducer.publishEvent(event);
            } catch (Exception e) {
                log.warn("Kafka publish failed (non-critical): {}", e.getMessage());
            }
        }

        // Also dispatch to notification-service directly to guarantee email delivery without Kafka latency
        if (targetUserId.contains("@")) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                    java.util.Map<String, String> invitePayload = java.util.Map.of(
                            "toEmail", targetUserId,
                            "actorId", currentUserId != null ? currentUserId : "A collaborator",
                            "workbookId", workbookId
                    );
                    restTemplate.postForEntity("http://localhost:8085/api/v1/notifications/invite", invitePayload, java.util.Map.class);
                    log.info("Direct invite dispatch to notification-service succeeded for {}", targetUserId);
                } catch (Exception e) {
                    log.debug("Direct invite dispatch fallback skipped: {}", e.getMessage());
                }
            });
        }

        return permissionMapper.toDto(permission);
    }

    @Transactional
    public void removePermission(String workbookId, String targetUserId, String currentUserId) {
        Workbook workbook = workbookRepository.findById(workbookId)
                .orElseThrow(() -> new ResourceNotFoundException("Workbook", workbookId));

        requireOwnerAccess(workbook, currentUserId);

        permissionRepository.deleteByWorkbookIdAndUserId(workbookId, targetUserId);
        log.info("Removed permission for user {} on workbook {}", targetUserId, workbookId);
    }

    @Transactional(readOnly = true)
    public List<PermissionDto> getWorkbookPermissions(String workbookId, String currentUserId) {
        Workbook workbook = workbookRepository.findById(workbookId)
                .orElseThrow(() -> new ResourceNotFoundException("Workbook", workbookId));

        requireViewAccess(workbook, currentUserId);

        return permissionRepository.findByWorkbookId(workbookId)
                .stream()
                .map(permissionMapper::toDto)
                .collect(Collectors.toList());
    }
}
