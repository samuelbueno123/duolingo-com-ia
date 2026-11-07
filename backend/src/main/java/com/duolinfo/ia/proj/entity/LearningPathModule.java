package com.duolinfo.ia.proj.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "learning_path_modules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningPathModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String language;

    @Column(nullable = false, length = 16)
    private String cefrLevel; // A1, A2, B1, B2, C1

    @Column(nullable = false)
    private Integer moduleOrder;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false, length = 64)
    private String category; // "Fundamentos", "Tempos Verbais", "Conversação", etc.

    @Column(length = 128)
    private String competenciesFocused; // "Gramática, Vocabulário"

    @Column(nullable = false)
    @Builder.Default
    private Integer requiredXp = 50;
}
