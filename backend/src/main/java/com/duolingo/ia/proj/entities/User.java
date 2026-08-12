package com.duolingo.ia.proj.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer age;

    @ManyToOne
    @JoinColumn(name = "institution_id")
    private Institution institution;
}
