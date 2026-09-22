package com.collab.spreadsheet.sheet.repository;

import com.collab.spreadsheet.sheet.entity.ProtectedRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProtectedRangeRepository extends JpaRepository<ProtectedRange, String> {

    List<ProtectedRange> findBySheetId(String sheetId);

    void deleteBySheetId(String sheetId);
}
