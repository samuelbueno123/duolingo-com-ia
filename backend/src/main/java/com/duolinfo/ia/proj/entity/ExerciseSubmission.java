package com.duolinfo.ia.proj.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exercise_submissions")
@Getter
@Setter
@NoArgsConstructor
public class ExerciseSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private LearningExercise exercise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "answers_json", nullable = false, columnDefinition = "TEXT")
    private String answersJson;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "xp_earned", nullable = false)
    private int xpEarned;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();
}
