package com.collab.spreadsheet.sheet.mapper;

import com.collab.spreadsheet.sheet.dto.ProtectedRangeDto;
import com.collab.spreadsheet.sheet.entity.ProtectedRange;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProtectedRangeMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProtectedRangeDto toDto(ProtectedRange protectedRange) {
        if (protectedRange == null) {
            return null;
        }
        return ProtectedRangeDto.builder()
                .id(protectedRange.getId())
                .sheetId(protectedRange.getSheet() != null ? protectedRange.getSheet().getId() : null)
                .name(protectedRange.getName())
                .startRow(protectedRange.getStartRow())
                .startCol(protectedRange.getStartCol())
                .endRow(protectedRange.getEndRow())
                .endCol(protectedRange.getEndCol())
                .allowedUserIds(jsonToList(protectedRange.getAllowedUserIds()))
                .createdAt(protectedRange.getCreatedAt())
                .build();
    }

    public List<String> jsonToList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
