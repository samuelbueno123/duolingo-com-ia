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
    List<ModuleProgressDTO> modules
) {}
