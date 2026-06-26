package com.duolingo.ia.proj.entities;

import com.duolingo.ia.proj.entities.enums.QuestionType;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String statement;

    @Enumerated(EnumType.STRING)
    private QuestionType type;

    private Integer scoreValue;

    @ManyToOne
    private Task task;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<AnswerOption> options = new ArrayList<>();
}
