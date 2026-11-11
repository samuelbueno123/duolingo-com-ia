package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDateTime;

public record StudentMistakeResponseDTO(
    Long id,
    Long exerciseId,
    String question,
    String userWrongAnswer,
    String correctAnswer,
    String explanation,
    int timesReviewed,
    int consecutiveCorrect,
    boolean resolved,
    LocalDateTime lastReviewedAt,
    LocalDateTime createdAt
) {}
