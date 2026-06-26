package com.duolingo.ia.proj.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tb_students")
public class Student extends User {

    @ManyToMany
    @JoinTable(
            name = "tb_student_group",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<Group> groups = new HashSet<>();

    @OneToMany(mappedBy = "student")
    private List<Progress> progresses = new ArrayList<>();

    @OneToMany(mappedBy = "student")
    private List<TaskAttempt> attempts = new ArrayList<>();
}
