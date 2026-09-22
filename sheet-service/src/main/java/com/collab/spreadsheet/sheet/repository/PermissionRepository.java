package com.collab.spreadsheet.sheet.repository;

import com.collab.spreadsheet.sheet.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    List<Permission> findByWorkbookId(String workbookId);

    Optional<Permission> findByWorkbookIdAndUserId(String workbookId, String userId);

    boolean existsByWorkbookIdAndUserId(String workbookId, String userId);

    void deleteByWorkbookIdAndUserId(String workbookId, String userId);
}
