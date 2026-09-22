package com.collab.spreadsheet.sheet.service;

import com.collab.spreadsheet.common.exception.ResourceNotFoundException;
import com.collab.spreadsheet.sheet.dto.ProtectedRangeDto;
import com.collab.spreadsheet.sheet.dto.ProtectedRangeRequest;
import com.collab.spreadsheet.sheet.entity.ProtectedRange;
import com.collab.spreadsheet.sheet.entity.Sheet;
import com.collab.spreadsheet.sheet.mapper.ProtectedRangeMapper;
import com.collab.spreadsheet.sheet.repository.ProtectedRangeRepository;
import com.collab.spreadsheet.sheet.repository.SheetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProtectedRangeService {

    private final ProtectedRangeRepository protectedRangeRepository;
    private final SheetRepository sheetRepository;
    private final PermissionService permissionService;
    private final ProtectedRangeMapper protectedRangeMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ProtectedRangeDto createProtectedRange(String sheetId, ProtectedRangeRequest request, String userId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet", sheetId));

        permissionService.requireEditAccess(sheet.getWorkbook(), userId);

        String allowedJson = "[]";
        if (request.getAllowedUserIds() != null && !request.getAllowedUserIds().isEmpty()) {
            try {
                allowedJson = objectMapper.writeValueAsString(request.getAllowedUserIds());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize allowed user ids: {}", e.getMessage());
            }
        }

        ProtectedRange range = ProtectedRange.builder()
                .sheet(sheet)
                .name(request.getName())
                .startRow(request.getStartRow())
                .startCol(request.getStartCol())
                .endRow(request.getEndRow())
                .endCol(request.getEndCol())
                .allowedUserIds(allowedJson)
                .build();

        range = protectedRangeRepository.save(range);
        log.info("Protected range '{}' created for sheet {} by user {}", range.getName(), sheetId, userId);

        return protectedRangeMapper.toDto(range);
    }

    @Transactional(readOnly = true)
    public List<ProtectedRangeDto> getProtectedRanges(String sheetId, String userId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet", sheetId));

        permissionService.requireViewAccess(sheet.getWorkbook(), userId);

        return protectedRangeRepository.findBySheetId(sheetId)
                .stream()
                .map(protectedRangeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteProtectedRange(String id, String userId) {
        ProtectedRange range = protectedRangeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProtectedRange", id));

        permissionService.requireEditAccess(range.getSheet().getWorkbook(), userId);
        protectedRangeRepository.delete(range);
        log.info("Deleted protected range {} by user {}", id, userId);
    }

    @Transactional(readOnly = true)
    public boolean isCellProtectedForUser(String sheetId, int row, int col, String userId) {
        List<ProtectedRange> ranges = protectedRangeRepository.findBySheetId(sheetId);
        for (ProtectedRange range : ranges) {
            if (row >= range.getStartRow() && row <= range.getEndRow() &&
                col >= range.getStartCol() && col <= range.getEndCol()) {
                
                List<String> allowed = parseAllowedUserIds(range.getAllowedUserIds());
                if (!allowed.contains(userId)) {
                    return true; // Protected and user is NOT allowed
                }
            }
        }
        return false;
    }

    private List<String> parseAllowedUserIds(String json) {
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
