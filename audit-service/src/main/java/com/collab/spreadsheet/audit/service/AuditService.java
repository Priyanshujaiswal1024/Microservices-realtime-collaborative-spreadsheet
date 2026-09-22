package com.collab.spreadsheet.audit.service;

import com.collab.spreadsheet.audit.dto.*;
import com.collab.spreadsheet.audit.entity.AuditEvent;
import com.collab.spreadsheet.audit.entity.Snapshot;
import com.collab.spreadsheet.audit.mapper.AuditMapper;
import com.collab.spreadsheet.audit.repository.AuditEventRepository;
import com.collab.spreadsheet.audit.repository.SnapshotRepository;
import com.collab.spreadsheet.common.dto.PageResponse;
import com.collab.spreadsheet.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final SnapshotRepository snapshotRepository;
    private final AuditMapper auditMapper;

    @Transactional(readOnly = true)
    public PageResponse<AuditEventDto> getWorkbookHistory(String workbookId, Pageable pageable) {
        Page<AuditEvent> page = auditEventRepository.findByWorkbookIdOrderByCreatedAtDesc(workbookId, pageable);
        List<AuditEventDto> content = auditMapper.toDtoList(page.getContent());

        return PageResponse.<AuditEventDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventDto> getSheetHistory(String sheetId, Pageable pageable) {
        Page<AuditEvent> page = auditEventRepository.findBySheetIdOrderByCreatedAtDesc(sheetId, pageable);
        List<AuditEventDto> content = auditMapper.toDtoList(page.getContent());

        return PageResponse.<AuditEventDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public SnapshotDto createSnapshot(String workbookId, CreateSnapshotRequest request, String userId) {
        log.info("Creating snapshot '{}' for workbook {} by user {}", request.getLabel(), workbookId, userId);

        Snapshot snapshot = Snapshot.builder()
                .workbookId(workbookId)
                .label(request.getLabel())
                .description(request.getDescription())
                .createdBy(userId)
                .snapshotData(request.getSnapshotData())
                .build();

        snapshot = snapshotRepository.save(snapshot);
        return auditMapper.toDto(snapshot);
    }

    @Transactional(readOnly = true)
    public List<SnapshotDto> getWorkbookSnapshots(String workbookId) {
        List<Snapshot> snapshots = snapshotRepository.findByWorkbookIdOrderByCreatedAtDesc(workbookId);
        return auditMapper.toSnapshotDtoList(snapshots);
    }

    @Transactional(readOnly = true)
    public RestoreResponse restoreWorkbook(String workbookId, RestoreSnapshotRequest request, String userId) {
        log.info("Restoring workbook {} requested by user {}", workbookId, userId);

        if (request.getSnapshotId() != null) {
            Snapshot snapshot = snapshotRepository.findById(request.getSnapshotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Snapshot", request.getSnapshotId()));

            return RestoreResponse.builder()
                    .workbookId(workbookId)
                    .restoredFromSnapshotId(snapshot.getId())
                    .targetTimestamp(snapshot.getCreatedAt())
                    .replayedEventsCount(0)
                    .snapshotData(snapshot.getSnapshotData())
                    .message("Successfully loaded snapshot checkpoint: " + snapshot.getLabel())
                    .build();
        }

        Instant targetTs = request.getTargetTimestamp() != null ? request.getTargetTimestamp() : Instant.now();
        List<AuditEvent> events = auditEventRepository.findByWorkbookIdAndCreatedAtLessThanEqualOrderByCreatedAtAsc(workbookId, targetTs);

        return RestoreResponse.builder()
                .workbookId(workbookId)
                .targetTimestamp(targetTs)
                .replayedEventsCount(events.size())
                .message("Replayed " + events.size() + " historical events up to timestamp: " + targetTs)
                .build();
    }
}
