package com.duolingo.ia.proj.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tb_progress",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"student_id", "course_id"})
        }
)
public class Progress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer accumulatedScore;
    private Double completionPercentage;
    private Boolean active;

    private LocalDateTime enrolledAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    private Student student;

    @ManyToOne
    private Course course;
}
