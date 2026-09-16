package com.duolinfo.ia.proj.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "students")
public class Student extends User {

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentLanguage> languages = new ArrayList<>();

    // Método utilitário para adicionar idiomas facilmente
    public void addLanguage(StudentLanguage language) {
        languages.add(language);
        language.setStudent(this);
    }
}