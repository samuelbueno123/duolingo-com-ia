package com.duolinfo.ia.proj.dto.learningpath;

import java.util.List;

public record DiagnosticQuestionDTO(
    int id,
    String cefrLevel, // A1, A2, B1, B2, C1
    String category, // "Gramática", "Vocabulário", "Leitura"
    String prompt,
    List<String> options,
    int correctIndex
) {}
