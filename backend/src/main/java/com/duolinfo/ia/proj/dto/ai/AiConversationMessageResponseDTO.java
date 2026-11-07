package com.duolinfo.ia.proj.dto.ai;

import java.time.LocalDateTime;

import com.duolinfo.ia.proj.entity.AiConversationMessage;

public record AiConversationMessageResponseDTO(
    Long id,
    Long sessionId,
    String sender,
    String content,
    String audioUrl,
    LocalDateTime timestamp,
    Boolean hasGrammarIssues,
    String grammarCorrection,
    String explanation,
    String betterNativeAlternative,
    Double fluencyScore
) {
    public static AiConversationMessageResponseDTO fromEntity(AiConversationMessage msg) {
        return new AiConversationMessageResponseDTO(
            msg.getId(),
            msg.getSessionId(),
            msg.getSender(),
            msg.getContent(),
            msg.getAudioUrl(),
            msg.getTimestamp(),
            msg.getHasGrammarIssues(),
            msg.getGrammarCorrection(),
            msg.getExplanation(),
            msg.getBetterNativeAlternative(),
            msg.getFluencyScore()
        );
    }
}
