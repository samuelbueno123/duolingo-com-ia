package com.duolingo.ia.proj.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_student_answers")
public class StudentAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String textAnswer;

    private Boolean correct;

    private Integer scoreObtained;

    @ManyToOne
    private TaskAttempt attempt;

    @ManyToOne
    private Question question;

    @ManyToOne
    private AnswerOption selectedOption;
}