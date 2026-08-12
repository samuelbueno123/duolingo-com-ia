package com.duolingo.ia.proj.entities;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_professors")
public class Professor extends User {

    private String academicDegree;
    private String specialization;

    @ManyToMany
    @JoinTable(
            name = "tb_professor_group",
            joinColumns = @JoinColumn(name = "professor_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<Group> groups = new HashSet<>();

    @ManyToMany(mappedBy = "professors")
    private Set<Course> courses = new HashSet<>();
}
