package com.duolinfo.ia.proj.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record ExplainMistakeRequestDTO(
    @NotBlank(message = "A pergunta é obrigatória.")
    String question,

    @NotBlank(message = "A resposta incorreta é obrigatória.")
    String wrongAnswer,

    @NotBlank(message = "A resposta correta é obrigatória.")
    String correctAnswer,

    String language
) {}
