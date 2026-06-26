package com.duolingo.ia.proj.entities;

import com.duolingo.ia.proj.entities.enums.DifficultyLevel;
import com.duolingo.ia.proj.entities.enums.TaskOrigin;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private TaskOrigin origin;

    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;

    @ManyToOne
    private Course course;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private List<Question> questions = new ArrayList<>();
}
