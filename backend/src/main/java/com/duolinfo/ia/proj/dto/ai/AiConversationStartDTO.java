package com.duolinfo.ia.proj.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record AiConversationStartDTO(
    @NotBlank(message = "O código do cenário é obrigatório.")
    String scenarioCode,

    @NotBlank(message = "O idioma de estudo é obrigatório.")
    String targetLanguage,

    String cefrLevel
) {}
