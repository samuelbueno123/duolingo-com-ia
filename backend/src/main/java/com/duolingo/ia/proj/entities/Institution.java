package com.duolingo.ia.proj.entities;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_institutions")
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String acronym; // exemplo: CEFET, IFRJ, UFRJ

    @OneToMany(mappedBy = "institution")
    private Set<User> users = new HashSet<>();
}