package com.collab.spreadsheet.audit.service;

import com.collab.spreadsheet.audit.dto.AuditEventDto;
import com.collab.spreadsheet.audit.dto.CreateSnapshotRequest;
import com.collab.spreadsheet.audit.dto.SnapshotDto;
import com.collab.spreadsheet.audit.entity.AuditEvent;
import com.collab.spreadsheet.audit.entity.Snapshot;
import com.collab.spreadsheet.audit.mapper.AuditMapper;
import com.collab.spreadsheet.audit.repository.AuditEventRepository;
import com.collab.spreadsheet.audit.repository.SnapshotRepository;
import com.collab.spreadsheet.common.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private SnapshotRepository snapshotRepository;

    @Mock
    private AuditMapper auditMapper;

    @InjectMocks
    private AuditService auditService;

    private AuditEvent sampleAuditEvent;
    private AuditEventDto sampleAuditEventDto;
    private Snapshot sampleSnapshot;
    private SnapshotDto sampleSnapshotDto;

    @BeforeEach
    void setUp() {
        sampleAuditEvent = AuditEvent.builder()
                .id("ae-1")
                .eventId("ev-123")
                .workbookId("wb-1")
                .sheetId("sheet-1")
                .row(0)
                .col(0)
                .eventType("CELL_EDIT")
                .actorId("user-1")
                .previousValue("Old")
                .newValue("New")
                .createdAt(Instant.now())
                .build();

        sampleAuditEventDto = AuditEventDto.builder()
                .id("ae-1")
                .eventId("ev-123")
                .workbookId("wb-1")
                .sheetId("sheet-1")
                .row(0)
                .col(0)
                .eventType("CELL_EDIT")
                .actorId("user-1")
                .previousValue("Old")
                .newValue("New")
                .createdAt(sampleAuditEvent.getCreatedAt())
                .build();

        sampleSnapshot = Snapshot.builder()
                .id("snap-1")
                .workbookId("wb-1")
                .label("End of Sprint Checkpoint")
                .createdBy("user-1")
                .createdAt(Instant.now())
                .snapshotData("{\"cells\": []}")
                .build();

        sampleSnapshotDto = SnapshotDto.builder()
                .id("snap-1")
                .workbookId("wb-1")
                .label("End of Sprint Checkpoint")
                .createdBy("user-1")
                .createdAt(sampleSnapshot.getCreatedAt())
                .snapshotData("{\"cells\": []}")
                .build();
    }

    @Test
    @DisplayName("Should retrieve paginated workbook history")
    void testGetWorkbookHistory() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditEvent> page = new PageImpl<>(List.of(sampleAuditEvent), pageable, 1);

        when(auditEventRepository.findByWorkbookIdOrderByCreatedAtDesc("wb-1", pageable)).thenReturn(page);
        when(auditMapper.toDtoList(List.of(sampleAuditEvent))).thenReturn(List.of(sampleAuditEventDto));

        PageResponse<AuditEventDto> response = auditService.getWorkbookHistory("wb-1", pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getEventId()).isEqualTo("ev-123");
        verify(auditEventRepository).findByWorkbookIdOrderByCreatedAtDesc("wb-1", pageable);
    }

    @Test
    @DisplayName("Should create snapshot checkpoint")
    void testCreateSnapshot() {
        CreateSnapshotRequest request = CreateSnapshotRequest.builder()
                .label("End of Sprint Checkpoint")
                .snapshotData("{\"cells\": []}")
                .build();

        when(snapshotRepository.save(any(Snapshot.class))).thenReturn(sampleSnapshot);
        when(auditMapper.toDto(any(Snapshot.class))).thenReturn(sampleSnapshotDto);

        SnapshotDto result = auditService.createSnapshot("wb-1", request, "user-1");

        assertThat(result).isNotNull();
        assertThat(result.getLabel()).isEqualTo("End of Sprint Checkpoint");
        verify(snapshotRepository).save(any(Snapshot.class));
    }
}
