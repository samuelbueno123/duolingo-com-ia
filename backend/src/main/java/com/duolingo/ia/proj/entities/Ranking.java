package com.duolingo.ia.proj.entities;

import com.duolingo.ia.proj.entities.enums.RankingType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_rankings")
public class Ranking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private RankingType type;

    private LocalDateTime generatedAt;

    @OneToMany(mappedBy = "ranking", cascade = CascadeType.ALL)
    private List<RankingEntry> entries = new ArrayList<>();
}
