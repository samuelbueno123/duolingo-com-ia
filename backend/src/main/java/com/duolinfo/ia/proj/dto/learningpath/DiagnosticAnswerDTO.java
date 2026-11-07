package com.duolinfo.ia.proj.dto.learningpath;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DiagnosticAnswerDTO(
    @NotBlank(message = "O idioma avaliado é obrigatório.")
    String language,

    @NotNull(message = "O mapa de respostas é obrigatório.")
    Map<Integer, Integer> answers // questionId -> selectedOptionIndex
) {}
