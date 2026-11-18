package com.duolinfo.ia.proj.dto.learningpath;

import java.time.LocalDateTime;
import java.util.List;

public record LearningPathDTO(
    Long id,
    Long studentId,
    String language,
    String currentCefrLevel,
    String targetCefrLevel,
    int dailyGoalMinutes,
    int currentStreakDays,
    double overallProgressPercent,
    // Competences (0-100)
    double grammarCompetence,
    double vocabularyCompetence,
    double listeningCompetence,
    double speakingCompetence,
    double readingCompetence,
    double writingCompetence,
    LocalDateTime lastDiagnosticAt,
    List<ModuleProgressDTO> modules,
    List<String> availableLanguages,
    java.util.Map<String, Object> activityMetrics
) {
    public LearningPathDTO(
            Long id,
            Long studentId,
            String language,
            String currentCefrLevel,
            String targetCefrLevel,
            int dailyGoalMinutes,
            int currentStreakDays,
            double overallProgressPercent,
            double grammarCompetence,
            double vocabularyCompetence,
            double listeningCompetence,
            double speakingCompetence,
            double readingCompetence,
            double writingCompetence,
            LocalDateTime lastDiagnosticAt,
            List<ModuleProgressDTO> modules) {
        this(
            id,
            studentId,
            language,
            currentCefrLevel,
            targetCefrLevel,
            dailyGoalMinutes,
            currentStreakDays,
            overallProgressPercent,
            grammarCompetence,
            vocabularyCompetence,
            listeningCompetence,
            speakingCompetence,
            readingCompetence,
            writingCompetence,
            lastDiagnosticAt,
            modules,
            language == null || language.isBlank() ? List.of() : List.of(language),
            java.util.Map.of()
        );
    }
}
