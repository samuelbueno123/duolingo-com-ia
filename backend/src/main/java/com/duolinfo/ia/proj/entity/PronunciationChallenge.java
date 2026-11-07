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
@Table(name = "pronunciation_challenges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PronunciationChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String language; // e.g. "Inglês", "Espanhol"

    @Column(nullable = false, length = 16)
    private String cefrLevel; // A1, A2, B1, B2

    @Column(nullable = false, length = 64)
    private String category; // "Cumprimentos", "Viagem", "Fonema /th/", etc.

    @Column(columnDefinition = "TEXT", nullable = false)
    private String phraseText;

    @Column(columnDefinition = "TEXT")
    private String phoneticGuide; // e.g. "/wɜːld waɪd web/" or simplified transcription

    @Column(columnDefinition = "TEXT", nullable = false)
    private String translation;

    @Column(columnDefinition = "TEXT")
    private String phoneticTips; // e.g. "Atente-se ao som de 'th' com a ponta da língua entre os dentes"

    @Column(nullable = false)
    @Builder.Default
    private Integer difficultyScore = 1; // 1 to 5
}
