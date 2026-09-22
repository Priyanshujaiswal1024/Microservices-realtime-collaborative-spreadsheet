package com.collab.spreadsheet.audit.repository;

import com.collab.spreadsheet.audit.entity.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, String> {

    List<Snapshot> findByWorkbookIdOrderByCreatedAtDesc(String workbookId);
}
