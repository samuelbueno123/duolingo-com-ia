package com.duolinfo.ia.proj.dto.ai;

import java.util.List;

public record AiScenarioDTO(
    String code,
    String title,
    String description,
    String category,
    String cefrLevel,
    String personaRole,
    String userRole,
    String initialGreeting,
    List<String> targetPhrases
) {}
