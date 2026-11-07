package com.duolinfo.ia.proj.dto.community;

import jakarta.validation.constraints.NotBlank;

public record CommunitySubmissionRequestDTO(
    @NotBlank(message = "O idioma é obrigatório.")
    String language,

    @NotBlank(message = "O título ou tema do exercício é obrigatório.")
    String promptTitle,

    @NotBlank(message = "O conteúdo escrito não pode estar vazio.")
    String content,

    String audioUrl,
    String targetLevel,
    Boolean isPublic
) {}
