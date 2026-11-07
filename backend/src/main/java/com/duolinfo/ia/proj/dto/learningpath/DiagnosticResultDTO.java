package com.duolinfo.ia.proj.dto.learningpath;

import java.util.List;

public record DiagnosticResultDTO(
    String language,
    String determinedCefrLevel,
    int correctCount,
    int totalQuestions,
    double scorePercentage,
    String diagnosticSummary,
    List<String> recommendedNextModules
) {}
