package com.duolingo.ia.proj.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_contents")
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    private Integer orderIndex;

    @ManyToOne
    private Course course;
}
