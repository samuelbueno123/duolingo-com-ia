package com.duolinfo.ia.proj.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "student_mistakes",
    indexes = {
        @Index(name = "idx_student_mistakes_student", columnList = "student_id"),
        @Index(name = "idx_student_mistakes_resolved", columnList = "student_id, resolved")
    }
)
public class StudentMistake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "exercise_id")
    private Long exerciseId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "user_wrong_answer", columnDefinition = "TEXT")
    private String userWrongAnswer;

    @Column(name = "correct_answer", nullable = false, columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "times_reviewed", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int timesReviewed = 0;

    @Column(name = "consecutive_correct", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int consecutiveCorrect = 0;

    @Column(name = "resolved", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean resolved = false;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
