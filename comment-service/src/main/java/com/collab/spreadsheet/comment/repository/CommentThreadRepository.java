package com.collab.spreadsheet.comment.repository;

import com.collab.spreadsheet.comment.entity.CommentThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentThreadRepository extends JpaRepository<CommentThread, String> {

    List<CommentThread> findByWorkbookIdOrderByCreatedAtDesc(String workbookId);

    List<CommentThread> findBySheetIdOrderByCreatedAtDesc(String sheetId);

    List<CommentThread> findBySheetIdAndRowAndCol(String sheetId, Integer row, Integer col);
}
