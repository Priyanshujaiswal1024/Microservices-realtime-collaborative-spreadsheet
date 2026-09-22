package com.collab.spreadsheet.notification.mapper;

import com.collab.spreadsheet.notification.dto.NotificationDto;
import com.collab.spreadsheet.notification.entity.Notification;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class NotificationMapper {

    public NotificationDto toDto(Notification entity) {
        if (entity == null) {
            return null;
        }
        return NotificationDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .actorId(entity.getActorId())
                .type(entity.getType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .link(entity.getLink())
                .workbookId(entity.getWorkbookId())
                .sheetId(entity.getSheetId())
                .read(entity.getRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<NotificationDto> toDtoList(List<Notification> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
