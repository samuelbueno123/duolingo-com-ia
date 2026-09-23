package com.duolinfo.ia.proj.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "students")
public class Student extends User {

    /*
     * Idiomas estudados pelo aluno.
     */
    @OneToMany(
        mappedBy = "student",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<StudentLanguage> languages = new ArrayList<>();

    /*
     * Histórico de matrículas do aluno.
     */
    @OneToMany(
        mappedBy = "student",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<Enrollment> enrollments = new ArrayList<>();

    /*
     * Adiciona um idioma ao aluno.
     */
    public void addLanguage(StudentLanguage language) {
        if (language == null) {
            return;
        }

        languages.add(language);
        language.setStudent(this);
    }

    /*
     * Remove um idioma do aluno.
     */
    public void removeLanguage(StudentLanguage language) {
        if (language == null) {
            return;
        }

        languages.remove(language);
        language.setStudent(null);
    }

    /*
     * Adiciona uma matrícula ao aluno.
     */
    public void addEnrollment(Enrollment enrollment) {
        if (enrollment == null) {
            return;
        }

        enrollments.add(enrollment);
        enrollment.setStudent(this);
    }

    /*
     * Remove uma matrícula do aluno.
     */
    public void removeEnrollment(Enrollment enrollment) {
        if (enrollment == null) {
            return;
        }

        enrollments.remove(enrollment);
        enrollment.setStudent(null);
    }
}