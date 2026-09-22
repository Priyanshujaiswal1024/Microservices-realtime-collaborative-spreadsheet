package com.collab.spreadsheet.comment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comment_threads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentThread {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "workbook_id", nullable = false)
    private String workbookId;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "cell_row", nullable = false)
    private Integer row;

    @Column(name = "cell_col", nullable = false)
    private Integer col;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(name = "initial_content", nullable = false, columnDefinition = "TEXT")
    private String initialContent;

    @Column(nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<CommentReply> replies = new ArrayList<>();
}
