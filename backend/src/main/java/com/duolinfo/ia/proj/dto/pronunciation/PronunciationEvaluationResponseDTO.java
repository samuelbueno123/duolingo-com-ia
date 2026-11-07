package com.duolinfo.ia.proj.dto.pronunciation;

import java.time.LocalDateTime;
import java.util.List;

public record PronunciationEvaluationResponseDTO(
    Long attemptId,
    Long challengeId,
    String expectedPhrase,
    String spokenText,
    double accuracyScore,
    double completenessScore,
    double finalScore,
    int xpEarned,
    String feedbackSummary,
    List<WordPronunciationFeedbackDTO> wordBreakdown,
    LocalDateTime attemptedAt
) {}
