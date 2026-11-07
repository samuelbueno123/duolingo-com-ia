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
@Table(name = "community_submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunitySubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long authorStudentId;

    @Column(nullable = false)
    private String authorName;

    @Column(nullable = false, length = 64)
    private String language;

    @Column(nullable = false)
    private String promptTitle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 256)
    private String audioUrl;

    @Column(length = 16)
    @Builder.Default
    private String targetLevel = "A2";

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "PENDING_REVIEW"; // "PENDING_REVIEW", "REVIEWED", "CLOSED"

    @Column(nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
