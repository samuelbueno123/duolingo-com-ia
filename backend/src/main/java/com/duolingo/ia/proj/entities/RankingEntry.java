package com.duolingo.ia.proj.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_ranking_entries")
public class RankingEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer position;
    private Integer score;

    @ManyToOne
    private Ranking ranking;

    @ManyToOne
    private Student student;

    @ManyToOne
    private Course course;

    @ManyToOne
    private Group group;
}
