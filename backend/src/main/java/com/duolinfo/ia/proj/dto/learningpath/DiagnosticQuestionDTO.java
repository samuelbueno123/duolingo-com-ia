package com.duolinfo.ia.proj.dto.learningpath;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record DiagnosticQuestionDTO(
    int id,
    String cefrLevel, // A1, A2, B1, B2, C1
    String category, // "Gramática", "Vocabulário", "Leitura"
    String prompt,
    List<String> options,
    @JsonIgnore int correctIndex,
    String mascotHelpMessage
) {
    public DiagnosticQuestionDTO(
        int id,
        String cefrLevel,
        String category,
        String prompt,
        List<String> options,
        int correctIndex
    ) {
        this(id, cefrLevel, category, prompt, options, correctIndex, null);
    }
}
