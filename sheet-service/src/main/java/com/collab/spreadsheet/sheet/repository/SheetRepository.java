package com.collab.spreadsheet.sheet.repository;

import com.collab.spreadsheet.sheet.entity.Sheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SheetRepository extends JpaRepository<Sheet, String> {

    List<Sheet> findByWorkbookIdOrderByPositionAsc(String workbookId);

    Optional<Sheet> findFirstByWorkbookIdOrderByPositionAsc(String workbookId);

    int countByWorkbookId(String workbookId);
}
