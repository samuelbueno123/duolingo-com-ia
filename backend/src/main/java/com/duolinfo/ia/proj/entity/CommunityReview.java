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
@Table(name = "community_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false)
    private Long reviewerStudentId;

    @Column(nullable = false)
    private String reviewerName;

    @Column(nullable = false, length = 32)
    private String reviewerFluencyLevel; // e.g. "B2", "C1", "C2", "NATIVO"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String overallFeedback;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String correctedContent;

    @Column(columnDefinition = "TEXT")
    private String nativeSpeakerTips;

    @Column(nullable = false)
    @Builder.Default
    private Integer rating = 5; // 1 to 5 stars

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
