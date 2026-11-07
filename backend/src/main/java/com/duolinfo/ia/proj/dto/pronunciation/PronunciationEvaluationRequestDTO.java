package com.duolinfo.ia.proj.dto.pronunciation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PronunciationEvaluationRequestDTO(
    @NotNull(message = "O ID do desafio é obrigatório.")
    Long challengeId,

    @NotBlank(message = "O texto reconhecido da fala não pode estar vazio.")
    String spokenText
) {}
