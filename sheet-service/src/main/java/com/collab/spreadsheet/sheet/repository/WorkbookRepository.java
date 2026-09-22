package com.collab.spreadsheet.sheet.repository;

import com.collab.spreadsheet.sheet.entity.Workbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkbookRepository extends JpaRepository<Workbook, String> {

    List<Workbook> findByOwnerIdOrderByUpdatedAtDesc(String ownerId);

    @Query("SELECT DISTINCT w FROM Workbook w LEFT JOIN w.permissions p WHERE w.ownerId = :userId OR (p.userId = :userId AND p.role IS NOT NULL) ORDER BY w.updatedAt DESC")
    List<Workbook> findAllAccessibleWorkbooks(@Param("userId") String userId);
}
