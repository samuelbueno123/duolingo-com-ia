package com.duolinfo.ia.proj.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
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
@Table(
    name = "students",
    indexes = {
        @Index(name = "idx_instituicao_aluno", columnList = "institution_id")
    }
)
public class Student extends User {

    /*
     * Instituição à qual o aluno está vinculado.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id")
    private Institution institution;

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

    @jakarta.persistence.Column(name = "current_streak", columnDefinition = "integer default 0")
    private Integer currentStreak = 0;

    @jakarta.persistence.Column(name = "longest_streak", columnDefinition = "integer default 0")
    private Integer longestStreak = 0;

    @jakarta.persistence.Column(name = "last_active_date")
    private java.time.LocalDate lastActiveDate;

    @jakarta.persistence.Column(name = "streak_freezes", columnDefinition = "integer default 2")
    private Integer streakFreezes = 2;

    @jakarta.persistence.Column(name = "today_xp_earned", columnDefinition = "integer default 0")
    private Integer todayXpEarned = 0;

    @jakarta.persistence.Column(name = "daily_xp_goal", columnDefinition = "integer default 50")
    private Integer dailyXpGoal = 50;

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