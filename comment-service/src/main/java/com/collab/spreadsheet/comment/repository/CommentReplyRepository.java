package com.collab.spreadsheet.comment.repository;

import com.collab.spreadsheet.comment.entity.CommentReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentReplyRepository extends JpaRepository<CommentReply, String> {

    List<CommentReply> findByThreadIdOrderByCreatedAtAsc(String threadId);
}
