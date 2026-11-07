package com.duolinfo.ia.proj.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "community_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reportedReviewId;

    @Column(nullable = false)
    private Long reportedByStudentId;

    @Column(nullable = false, length = 64)
    private String reason; // "OFENSIVO", "SPAM", "INCORRETO", "OUTRO"

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "PENDING"; // "PENDING", "RESOLVED", "DISMISSED"

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
