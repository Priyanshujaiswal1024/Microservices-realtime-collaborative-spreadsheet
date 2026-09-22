package com.collab.spreadsheet.sheet.service;

import com.collab.spreadsheet.common.dto.PermissionRole;
import com.collab.spreadsheet.common.dto.WorkbookVisibility;
import com.collab.spreadsheet.common.exception.ForbiddenException;
import com.collab.spreadsheet.sheet.dto.CreateWorkbookRequest;
import com.collab.spreadsheet.sheet.dto.WorkbookDto;
import com.collab.spreadsheet.sheet.entity.Permission;
import com.collab.spreadsheet.sheet.entity.Sheet;
import com.collab.spreadsheet.sheet.entity.Workbook;
import com.collab.spreadsheet.sheet.kafka.KafkaSheetLifecycleProducer;
import com.collab.spreadsheet.sheet.mapper.WorkbookMapper;
import com.collab.spreadsheet.sheet.repository.PermissionRepository;
import com.collab.spreadsheet.sheet.repository.SheetRepository;
import com.collab.spreadsheet.sheet.repository.WorkbookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkbookServiceTest {

    @Mock
    private WorkbookRepository workbookRepository;

    @Mock
    private SheetRepository sheetRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private WorkbookMapper workbookMapper;

    @Mock
    private KafkaSheetLifecycleProducer lifecycleProducer;

    @InjectMocks
    private WorkbookService workbookService;

    private Workbook sampleWorkbook;
    private WorkbookDto sampleWorkbookDto;

    @BeforeEach
    void setUp() {
        sampleWorkbook = Workbook.builder()
                .id("wb-123")
                .ownerId("user-1")
                .title("Q1 Financial Model")
                .visibility(WorkbookVisibility.PRIVATE)
                .sheets(new ArrayList<>())
                .permissions(new ArrayList<>())
                .build();

        sampleWorkbookDto = WorkbookDto.builder()
                .id("wb-123")
                .ownerId("user-1")
                .title("Q1 Financial Model")
                .visibility(WorkbookVisibility.PRIVATE)
                .userRole(PermissionRole.OWNER)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a workbook with initial sheet and owner permission")
    void testCreateWorkbook() {
        CreateWorkbookRequest request = CreateWorkbookRequest.builder()
                .title("Q1 Financial Model")
                .visibility(WorkbookVisibility.PRIVATE)
                .initialSheetName("Summary")
                .build();

        when(workbookRepository.save(any(Workbook.class))).thenReturn(sampleWorkbook);
        when(sheetRepository.save(any(Sheet.class))).thenAnswer(i -> i.getArgument(0));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(i -> i.getArgument(0));
        when(workbookMapper.toDto(any(Workbook.class))).thenReturn(sampleWorkbookDto);

        WorkbookDto result = workbookService.createWorkbook(request, "user-1");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Q1 Financial Model");
        assertThat(result.getUserRole()).isEqualTo(PermissionRole.OWNER);

        verify(workbookRepository).save(any(Workbook.class));
        verify(sheetRepository).save(any(Sheet.class));
        verify(permissionRepository).save(any(Permission.class));
        verify(lifecycleProducer).publishEvent(any());
    }

    @Test
    @DisplayName("Should retrieve workbook and attach requesting user's role")
    void testGetWorkbook() {
        when(workbookRepository.findById("wb-123")).thenReturn(Optional.of(sampleWorkbook));
        when(workbookMapper.toDto(sampleWorkbook)).thenReturn(sampleWorkbookDto);
        when(permissionService.getUserRole(sampleWorkbook, "user-1")).thenReturn(PermissionRole.OWNER);

        WorkbookDto result = workbookService.getWorkbook("wb-123", "user-1");

        assertThat(result).isNotNull();
        assertThat(result.getUserRole()).isEqualTo(PermissionRole.OWNER);
        verify(permissionService).requireViewAccess(sampleWorkbook, "user-1");
    }

    @Test
    @DisplayName("Should fail when unauthorized user attempts to delete workbook")
    void testDeleteWorkbookUnauthorized() {
        when(workbookRepository.findById("wb-123")).thenReturn(Optional.of(sampleWorkbook));
        doThrow(new ForbiddenException("Only the workbook owner can perform this operation"))
                .when(permissionService).requireOwnerAccess(sampleWorkbook, "user-2");

        assertThatThrownBy(() -> workbookService.deleteWorkbook("wb-123", "user-2"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only the workbook owner");
    }
}
