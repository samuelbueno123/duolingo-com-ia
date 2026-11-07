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
@Table(name = "ai_conversation_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false, length = 16)
    private String sender; // "USER" or "AI"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 256)
    private String audioUrl;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // AI Pedagogical feedback (when sender == "USER")
    @Builder.Default
    private Boolean hasGrammarIssues = false;

    @Column(columnDefinition = "TEXT")
    private String grammarCorrection;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(columnDefinition = "TEXT")
    private String betterNativeAlternative;

    private Double fluencyScore;
}
