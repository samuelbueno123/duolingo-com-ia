package com.duolinfo.ia.proj.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "classes",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_class_school_year_name_shift",
            columnNames = {
                "school_id",
                "school_year",
                "name",
                "shift"
            }
        )
    }
)
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome da turma é obrigatório.")
    @Column(nullable = false)
    private String name;

    /**
     * Exemplo:
     * 2026
     * 2026/1
     * 2026/2
     */
    private String schoolYear;

    /**
     * Exemplo:
     * MANHÃ
     * TARDE
     * NOITE
     * INTEGRAL
     */
    private String shift;

    /**
     * Exemplo:
     * 1º Ano
     * 2º Ano Técnico
     * Módulo 2
     */
    private String gradeLevel;

    /**
     * Escola responsável pela turma.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    /**
     * Professores que já foram ou são responsáveis pela turma.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(
        mappedBy = "schoolClass",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    private List<ClassTeacherAssignment> teacherAssignments = new ArrayList<>();

    /**
     * Alunos matriculados na turma.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(
        mappedBy = "schoolClass",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}