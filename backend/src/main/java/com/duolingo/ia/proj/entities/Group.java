package com.duolingo.ia.proj.entities;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_groups")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany(mappedBy = "groups")
    private Set<Student> students = new HashSet<>();

    @ManyToMany(mappedBy = "groups")
    private Set<Professor> professors = new HashSet<>();

    @ManyToOne
    private Institution institution;
}
