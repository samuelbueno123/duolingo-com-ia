package com.duolinfo.ia.proj.dto.pronunciation;

public record WordPronunciationFeedbackDTO(
    String expectedWord,
    String spokenWord,
    String status, // "PERFECT", "ACCEPTABLE", "MISPRONOUNCED", "OMITTED", "EXTRA"
    double similarityScore,
    String phoneticTip
) {}
