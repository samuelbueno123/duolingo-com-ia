package com.duolinfo.ia.proj.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record AiConversationMessageRequestDTO(
    @NotBlank(message = "A mensagem não pode estar vazia.")
    String message,

    String audioUrl
) {}
