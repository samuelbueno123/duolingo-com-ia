package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDateTime;

public record VocabularyResponseDTO(
    Long id,
    String language,
    String term,
    String translation,
    String partOfSpeech,
    String exampleSentence,
    String masteryLevel,
    int timesPracticed,
    LocalDateTime createdAt
) {}
