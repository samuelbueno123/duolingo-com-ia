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
@Table(name = "student_learning_paths")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentLearningPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false, length = 64)
    private String language; // "Inglês", "Espanhol"

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String currentCefrLevel = "A1"; // A1, A2, B1, B2, C1, C2

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String targetCefrLevel = "B2";

    @Column(nullable = false)
    @Builder.Default
    private Integer dailyGoalMinutes = 15; // 5 (Casual), 15 (Regular), 30 (Intenso)

    @Column(nullable = false)
    @Builder.Default
    private Integer currentStreakDays = 1;

    @Column(nullable = false)
    @Builder.Default
    private Double overallProgressPercent = 0.0;

    // CEFR Competence Matrix (0.0 to 100.0)
    @Column(nullable = false)
    @Builder.Default
    private Double grammarCompetence = 20.0;

    @Column(nullable = false)
    @Builder.Default
    private Double vocabularyCompetence = 25.0;

    @Column(nullable = false)
    @Builder.Default
    private Double listeningCompetence = 15.0;

    @Column(nullable = false)
    @Builder.Default
    private Double speakingCompetence = 15.0;

    @Column(nullable = false)
    @Builder.Default
    private Double readingCompetence = 30.0;

    @Column(nullable = false)
    @Builder.Default
    private Double writingCompetence = 20.0;

    private LocalDateTime lastDiagnosticAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
