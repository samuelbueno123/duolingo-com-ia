package com.duolinfo.ia.proj.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
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
@Table(name = "teachers")
public class Teacher extends User {

    /*
     * Instituição à qual o professor pertence.
     *
     * Uma instituição pode ter vários professores.
     * Cada professor pertence a uma instituição.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    /*
     * Idiomas que o professor leciona.
     */
    @ElementCollection
    @CollectionTable(
        name = "teacher_taught_languages",
        joinColumns = @JoinColumn(name = "teacher_id")
    )
    @Column(name = "language_name", nullable = false)
    private List<String> taughtLanguages = new ArrayList<>();

    /*
     * Áreas de especialização do professor.
     */
    @ElementCollection
    @CollectionTable(
        name = "teacher_specialization_areas",
        joinColumns = @JoinColumn(name = "teacher_id")
    )
    @Column(name = "specialization_area", nullable = false)
    private List<String> specializationAreas = new ArrayList<>();

    /*
     * Biografia / currículo do professor.
     */
    @Lob
    @Column(name = "bibliography", columnDefinition = "TEXT")
    private String bibliography;

    /*
     * Documentos do professor.
     */
    @OneToMany(
        mappedBy = "teacher",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<TeacherDocument> documents = new ArrayList<>();

    /*
     * Histórico de turmas atribuídas ao professor.
     *
     * Não usamos cascade/orphanRemoval aqui porque
     * queremos preservar o histórico de atribuições.
     */
    @OneToMany(mappedBy = "teacher")
    private List<ClassTeacherAssignment> classAssignments = new ArrayList<>();

    public void addTaughtLanguage(String language) {
        if (language != null && !language.trim().isEmpty()) {
            this.taughtLanguages.add(language.trim());
        }
    }

    public void removeTaughtLanguage(String language) {
        if (language != null) {
            this.taughtLanguages.remove(language);
        }
    }

    public void addSpecializationArea(String area) {
        if (area != null && !area.trim().isEmpty()) {
            this.specializationAreas.add(area.trim());
        }
    }

    public void removeSpecializationArea(String area) {
        if (area != null) {
            this.specializationAreas.remove(area);
        }
    }

    public void addDocument(TeacherDocument document) {
        if (document == null) {
            return;
        }

        documents.add(document);
        document.setTeacher(this);
    }

    public void removeDocument(TeacherDocument document) {
        if (document == null) {
            return;
        }

        documents.remove(document);
        document.setTeacher(null);
    }

    public void addClassAssignment(ClassTeacherAssignment assignment) {
        if (assignment == null) {
            return;
        }

        classAssignments.add(assignment);
        assignment.setTeacher(this);
    }

    public void removeClassAssignment(ClassTeacherAssignment assignment) {
        if (assignment == null) {
            return;
        }

        classAssignments.remove(assignment);
        assignment.setTeacher(null);
    }
}