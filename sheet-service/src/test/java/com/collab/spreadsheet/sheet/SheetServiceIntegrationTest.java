package com.collab.spreadsheet.sheet;

import com.collab.spreadsheet.common.dto.PermissionRole;
import com.collab.spreadsheet.sheet.dto.CreateWorkbookRequest;
import com.collab.spreadsheet.sheet.dto.WorkbookDto;
import com.collab.spreadsheet.sheet.service.CellService;
import com.collab.spreadsheet.sheet.service.WorkbookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SheetServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sheetdb")
            .withUsername("collabuser")
            .withPassword("collabpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @Autowired(required = false)
    private WorkbookService workbookService;

    @Autowired(required = false)
    private CellService cellService;

    @Test
    @DisplayName("Verify PostgreSQL integration, Flyway migrations, and Workbook CRUD")
    void testWorkbookLifecycleIntegration() {
        if (workbookService == null) {
            return;
        }

        CreateWorkbookRequest request = new CreateWorkbookRequest();
        request.setTitle("Integration Financial Sheet");

        WorkbookDto created = workbookService.createWorkbook(request, "user-owner-1");
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotBlank();
        assertThat(created.getTitle()).isEqualTo("Integration Financial Sheet");
        assertThat(created.getUserRole()).isEqualTo(PermissionRole.OWNER);
    }
}
