package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClassAnnouncementRequestDTO(
    @NotBlank(message = "O título do comunicado é obrigatório.")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres.")
    String title,

    @NotBlank(message = "O conteúdo do comunicado é obrigatório.")
    String content,

    String priority, // NORMAL, URGENT

    Boolean pinned
) {}
