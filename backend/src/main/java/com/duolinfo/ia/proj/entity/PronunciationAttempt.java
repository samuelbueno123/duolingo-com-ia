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
@Table(name = "pronunciation_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PronunciationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long challengeId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String spokenText;

    @Column(nullable = false)
    private Double accuracyScore; // 0.0 to 100.0

    @Column(nullable = false)
    private Double completenessScore; // 0.0 to 100.0

    @Column(nullable = false)
    private Double finalScore; // weighted overall

    @Column(columnDefinition = "TEXT")
    private String wordBreakdownJson; // detailed word by word breakdown

    @Column(columnDefinition = "TEXT")
    private String feedbackSummary;

    @Column(nullable = false)
    private LocalDateTime attemptedAt;
}
