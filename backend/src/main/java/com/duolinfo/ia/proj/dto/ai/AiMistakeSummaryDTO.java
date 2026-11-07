package com.duolinfo.ia.proj.dto.ai;

import java.time.LocalDateTime;

public record AiMistakeSummaryDTO(
    Long messageId,
    Long sessionId,
    String originalText,
    String correctedText,
    String explanation,
    String betterNativeAlternative,
    LocalDateTime timestamp
) {}
