package com.collab.spreadsheet.audit.mapper;

import com.collab.spreadsheet.audit.dto.AuditEventDto;
import com.collab.spreadsheet.audit.dto.SnapshotDto;
import com.collab.spreadsheet.audit.entity.AuditEvent;
import com.collab.spreadsheet.audit.entity.Snapshot;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuditMapper {

    public AuditEventDto toDto(AuditEvent entity) {
        if (entity == null) {
            return null;
        }
        return AuditEventDto.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .workbookId(entity.getWorkbookId())
                .sheetId(entity.getSheetId())
                .row(entity.getRow())
                .col(entity.getCol())
                .eventType(entity.getEventType())
                .actorId(entity.getActorId())
                .previousValue(entity.getPreviousValue())
                .newValue(entity.getNewValue())
                .hlcTimestamp(entity.getHlcTimestamp())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<AuditEventDto> toDtoList(List<AuditEvent> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    public SnapshotDto toDto(Snapshot entity) {
        if (entity == null) {
            return null;
        }
        return SnapshotDto.builder()
                .id(entity.getId())
                .workbookId(entity.getWorkbookId())
                .label(entity.getLabel())
                .description(entity.getDescription())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .snapshotData(entity.getSnapshotData())
                .build();
    }

    public List<SnapshotDto> toSnapshotDtoList(List<Snapshot> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
