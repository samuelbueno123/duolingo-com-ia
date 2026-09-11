package com.duolinfo.ia.proj.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    @NotBlank
    @Column(name = "institution", nullable = false)
    private String institution;

    @ElementCollection
    @CollectionTable(
        name = "teacher_taught_languages",
        joinColumns = @JoinColumn(name = "teacher_id")
    )
    @Column(name = "language_name", nullable = false)
    private List<String> taughtLanguages = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
        name = "teacher_specialization_areas",
        joinColumns = @JoinColumn(name = "teacher_id")
    )
    @Column(name = "specialization_area", nullable = false)
    private List<String> specializationAreas = new ArrayList<>();

    @Lob
    @Column(name = "bibliography", columnDefinition = "TEXT")
    private String bibliography;

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeacherDocument> documents = new ArrayList<>();

    public void addTaughtLanguage(String language) {
        if (language != null && !language.trim().isEmpty()) {
            this.taughtLanguages.add(language.trim());
        }
    }

    public void addSpecializationArea(String area) {
        if (area != null && !area.trim().isEmpty()) {
            this.specializationAreas.add(area.trim());
        }
    }

    public void addDocument(TeacherDocument document) {
        documents.add(document);
        document.setTeacher(this);
    }

    public void removeDocument(TeacherDocument document) {
        documents.remove(document);
        document.setTeacher(null);
    }
}
