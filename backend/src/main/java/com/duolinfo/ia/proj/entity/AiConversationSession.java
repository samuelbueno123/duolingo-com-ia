package com.duolinfo.ia.proj.entity;

import java.time.LocalDateTime;

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
@Table(name = "ai_conversation_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConversationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false, length = 64)
    private String scenarioCode;

    @Column(nullable = false)
    private String scenarioTitle;

    @Column(nullable = false, length = 64)
    private String targetLanguage;

    @Column(nullable = false, length = 16)
    private String cefrLevel;

    @Column(length = 128)
    private String personaRole;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalTurns = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer grammarCorrectionsCount = 0;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}
