package com.duolinfo.ia.proj.dto.srs;

import java.time.Instant;
import java.time.LocalDate;

public record SpacedReviewItemResponseDTO(
    Long id,
    String languageName,
    String term,
    String translation,
    String exampleSentence,
    String exampleTranslation,
    String proficiencyLevel,
    String tags,
    int repetitionNumber,
    int intervalDays,
    double easeFactor,
    LocalDate dueDate,
    boolean isDue,
    Instant lastReviewedAt,
    int totalReviews,
    int lapseCount,
    int xpEarned
) {
}
