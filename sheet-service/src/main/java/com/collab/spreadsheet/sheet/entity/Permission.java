package com.collab.spreadsheet.sheet.entity;

import com.collab.spreadsheet.common.dto.PermissionRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permission_user", columnList = "user_id"),
    @Index(name = "idx_permission_workbook", columnList = "workbook_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_workbook_user_permission", columnNames = {"workbook_id", "user_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workbook_id", nullable = false)
    private Workbook workbook;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "user_email", length = 100)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PermissionRole role = PermissionRole.VIEWER;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
