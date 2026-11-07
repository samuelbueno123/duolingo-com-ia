package com.duolinfo.ia.proj.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommunityReportRequestDTO(
    @NotNull(message = "O ID da avaliação denunciada é obrigatório.")
    Long reviewId,

    @NotBlank(message = "O motivo da denúncia é obrigatório.")
    String reason,

    String details
) {}
