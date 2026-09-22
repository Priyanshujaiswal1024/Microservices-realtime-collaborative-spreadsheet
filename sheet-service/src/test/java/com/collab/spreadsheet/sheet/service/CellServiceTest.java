package com.collab.spreadsheet.sheet.service;

import com.collab.spreadsheet.common.crdt.HybridLogicalClock;
import com.collab.spreadsheet.common.exception.ForbiddenException;
import com.collab.spreadsheet.sheet.dto.CellDto;
import com.collab.spreadsheet.sheet.entity.Cell;
import com.collab.spreadsheet.sheet.entity.Sheet;
import com.collab.spreadsheet.sheet.entity.Workbook;
import com.collab.spreadsheet.sheet.mapper.CellMapper;
import com.collab.spreadsheet.sheet.repository.CellRepository;
import com.collab.spreadsheet.sheet.repository.SheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CellServiceTest {

    @Mock
    private CellRepository cellRepository;

    @Mock
    private SheetRepository sheetRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private ProtectedRangeService protectedRangeService;

    @Mock
    private CellMapper cellMapper;

    @InjectMocks
    private CellService cellService;

    private Sheet sampleSheet;
    private Cell sampleCell;
    private CellDto sampleCellDto;

    @BeforeEach
    void setUp() {
        Workbook wb = Workbook.builder().id("wb-1").ownerId("user-1").build();
        sampleSheet = Sheet.builder().id("sheet-1").workbook(wb).name("Sheet1").build();

        sampleCell = Cell.builder()
                .id("c-1")
                .sheet(sampleSheet)
                .row(0)
                .col(0)
                .value("100")
                .dataType("NUMBER")
                .lastModifiedTs("1000:0:clientA")
                .lastModifiedBy("user-1")
                .version(1L)
                .build();

        sampleCellDto = CellDto.builder()
                .id("c-1")
                .sheetId("sheet-1")
                .row(0)
                .col(0)
                .value("100")
                .dataType("NUMBER")
                .build();
    }

    @Test
    @DisplayName("Should upsert new cell when not present")
    void testUpsertNewCell() {
        when(sheetRepository.findById("sheet-1")).thenReturn(Optional.of(sampleSheet));
        when(protectedRangeService.isCellProtectedForUser("sheet-1", 0, 0, "user-1")).thenReturn(false);
        when(cellRepository.findBySheetIdAndRowAndCol("sheet-1", 0, 0)).thenReturn(Optional.empty());
        when(cellRepository.save(any(Cell.class))).thenReturn(sampleCell);
        when(cellMapper.toDto(any(Cell.class))).thenReturn(sampleCellDto);

        CellDto result = cellService.upsertCell("sheet-1", 0, 0, "100", "NUMBER", null, "1000:0:clientA", "user-1");

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo("100");
        verify(cellRepository).save(any(Cell.class));
    }

    @Test
    @DisplayName("Should apply newer HLC update and ignore older HLC update on existing cell")
    void testLwwMergeOnExistingCell() {
        when(sheetRepository.findById("sheet-1")).thenReturn(Optional.of(sampleSheet));
        when(protectedRangeService.isCellProtectedForUser("sheet-1", 0, 0, "user-1")).thenReturn(false);
        when(cellRepository.findBySheetIdAndRowAndCol("sheet-1", 0, 0)).thenReturn(Optional.of(sampleCell));
        when(cellRepository.save(any(Cell.class))).thenAnswer(i -> i.getArgument(0));
        when(cellMapper.toDto(any(Cell.class))).thenAnswer(i -> {
            Cell c = i.getArgument(0);
            return CellDto.builder().value(c.getValue()).build();
        });

        // 1. Newer HLC update -> should apply
        CellDto updated = cellService.upsertCell("sheet-1", 0, 0, "200", "NUMBER", null, "2000:0:clientB", "user-1");
        assertThat(updated.getValue()).isEqualTo("200");

        // 2. Older HLC update -> should NOT overwrite newer value
        CellDto ignored = cellService.upsertCell("sheet-1", 0, 0, "50", "NUMBER", null, "500:0:clientC", "user-1");
        assertThat(ignored.getValue()).isEqualTo("200");
    }

    @Test
    @DisplayName("Should prevent edit if cell falls in a protected range for the user")
    void testProtectedRangeRejection() {
        when(sheetRepository.findById("sheet-1")).thenReturn(Optional.of(sampleSheet));
        when(protectedRangeService.isCellProtectedForUser("sheet-1", 5, 5, "user-2")).thenReturn(true);

        assertThatThrownBy(() -> cellService.upsertCell("sheet-1", 5, 5, "Hacked", "TEXT", null, "3000:0:client", "user-2"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("protected range");
    }
}
