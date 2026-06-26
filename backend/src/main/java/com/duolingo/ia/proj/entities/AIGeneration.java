package com.duolingo.ia.proj.entities;

import com.duolingo.ia.proj.entities.enums.DifficultyLevel;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_ai_generations")
public class AIGeneration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;

    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String generatedContent;

    private LocalDateTime generatedAt;

    @ManyToOne
    private Professor professor;

    @OneToOne
    private Task generatedTask;
}
