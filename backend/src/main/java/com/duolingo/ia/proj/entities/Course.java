package com.duolingo.ia.proj.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tb_courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToMany
    @JoinTable(
            name = "tb_course_professor",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "professor_id")
    )
    private Set<Professor> professors = new HashSet<>();

    @OneToMany(mappedBy = "course")
    private List<Content> contents = new ArrayList<>();

    @OneToMany(mappedBy = "course")
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "course")
    private List<Progress> progresses = new ArrayList<>();
}