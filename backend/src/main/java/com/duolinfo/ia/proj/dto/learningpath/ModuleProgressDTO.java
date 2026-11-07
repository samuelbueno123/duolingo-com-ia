package com.duolinfo.ia.proj.dto.learningpath;

public record ModuleProgressDTO(
    Long moduleId,
    String language,
    String cefrLevel,
    int moduleOrder,
    String title,
    String description,
    String category,
    String competenciesFocused,
    int requiredXp,
    String status, // "LOCKED", "IN_PROGRESS", "COMPLETED"
    double scorePercent
) {}
