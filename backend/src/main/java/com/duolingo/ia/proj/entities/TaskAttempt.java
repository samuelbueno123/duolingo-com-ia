package com.duolingo.ia.proj.entities;

import com.duolingo.ia.proj.entities.enums.AttemptStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_task_attempts")
public class TaskAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer attemptNumber;

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    private Integer score;

    @Enumerated(EnumType.STRING)
    private AttemptStatus status;

    @ManyToOne
    private Student student;

    @ManyToOne
    private Task task;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL)
    private List<StudentAnswer> answers = new ArrayList<>();
}
