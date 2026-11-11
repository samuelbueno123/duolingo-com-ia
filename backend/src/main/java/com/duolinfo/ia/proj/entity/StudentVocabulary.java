package com.duolinfo.ia.proj.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.UniqueConstraint;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "student_vocabularies",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_student_vocab_term",
            columnNames = { "student_id", "language", "term" }
        )
    },
    indexes = {
        @Index(name = "idx_vocab_student", columnList = "student_id"),
        @Index(name = "idx_vocab_student_lang", columnList = "student_id, language")
    }
)
public class StudentVocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "language", nullable = false, length = 60)
    private String language;

    @Column(name = "term", nullable = false, length = 120)
    private String term;

    @Column(name = "translation", nullable = false, length = 200)
    private String translation;

    @Column(name = "part_of_speech", length = 30)
    private String partOfSpeech;

    @Column(name = "example_sentence", columnDefinition = "TEXT")
    private String exampleSentence;

    @Column(name = "mastery_level", length = 30, columnDefinition = "varchar(30) default 'LEARNING'")
    @Builder.Default
    private String masteryLevel = "LEARNING";

    @Column(name = "times_practiced", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int timesPracticed = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
