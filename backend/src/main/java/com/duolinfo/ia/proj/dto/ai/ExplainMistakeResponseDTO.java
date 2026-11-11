package com.duolinfo.ia.proj.dto.ai;

import java.util.List;

public record ExplainMistakeResponseDTO(
    String explanation,
    String grammarRule,
    String memoryTip,
    List<String> similarExamples
) {}
