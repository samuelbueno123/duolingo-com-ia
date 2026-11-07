package com.duolinfo.ia.proj.dto.pronunciation;

import com.duolinfo.ia.proj.entity.PronunciationChallenge;

public record PronunciationChallengeDTO(
    Long id,
    String language,
    String cefrLevel,
    String category,
    String phraseText,
    String phoneticGuide,
    String translation,
    String phoneticTips,
    Integer difficultyScore
) {
    public static PronunciationChallengeDTO fromEntity(PronunciationChallenge c) {
        return new PronunciationChallengeDTO(
            c.getId(),
            c.getLanguage(),
            c.getCefrLevel(),
            c.getCategory(),
            c.getPhraseText(),
            c.getPhoneticGuide(),
            c.getTranslation(),
            c.getPhoneticTips(),
            c.getDifficultyScore()
        );
    }
}
