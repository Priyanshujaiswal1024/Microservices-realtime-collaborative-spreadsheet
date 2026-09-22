package com.collab.spreadsheet.sheet.repository;

import com.collab.spreadsheet.sheet.entity.Cell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CellRepository extends JpaRepository<Cell, String> {

    List<Cell> findBySheetId(String sheetId);

    Optional<Cell> findBySheetIdAndRowAndCol(String sheetId, Integer row, Integer col);

    @Query("SELECT c FROM Cell c WHERE c.sheet.id = :sheetId AND c.row >= :startRow AND c.row <= :endRow AND c.col >= :startCol AND c.col <= :endCol")
    List<Cell> findBySheetIdAndRange(
            @Param("sheetId") String sheetId,
            @Param("startRow") Integer startRow,
            @Param("endRow") Integer endRow,
            @Param("startCol") Integer startCol,
            @Param("endCol") Integer endCol);

    void deleteBySheetId(String sheetId);
}
