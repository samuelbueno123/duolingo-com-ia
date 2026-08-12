package com.duolingo.ia.proj.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_answer_options")
public class AnswerOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String text;

    private Boolean correct;

    @ManyToOne
    private Question question;
}
