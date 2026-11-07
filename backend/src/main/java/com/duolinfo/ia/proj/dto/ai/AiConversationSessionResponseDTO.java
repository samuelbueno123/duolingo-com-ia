package com.duolinfo.ia.proj.dto.ai;

import java.time.LocalDateTime;
import java.util.List;

import com.duolinfo.ia.proj.entity.AiConversationSession;

public record AiConversationSessionResponseDTO(
    Long id,
    Long studentId,
    String scenarioCode,
    String scenarioTitle,
    String targetLanguage,
    String cefrLevel,
    String personaRole,
    Boolean active,
    Integer totalTurns,
    Integer grammarCorrectionsCount,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    List<AiConversationMessageResponseDTO> messages
) {
    public static AiConversationSessionResponseDTO fromEntity(AiConversationSession session, List<AiConversationMessageResponseDTO> messages) {
        return new AiConversationSessionResponseDTO(
            session.getId(),
            session.getStudentId(),
            session.getScenarioCode(),
            session.getScenarioTitle(),
            session.getTargetLanguage(),
            session.getCefrLevel(),
            session.getPersonaRole(),
            session.getActive(),
            session.getTotalTurns(),
            session.getGrammarCorrectionsCount(),
            session.getStartedAt(),
            session.getEndedAt(),
            messages
        );
    }
}
