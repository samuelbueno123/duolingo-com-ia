package com.duolinfo.ia.proj.dto.community;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommunityReviewRequestDTO(
    @NotNull(message = "O ID da submissão é obrigatório.")
    Long submissionId,

    @NotBlank(message = "O feedback geral não pode estar vazio.")
    String overallFeedback,

    @NotBlank(message = "A versão corrigida do texto é obrigatória.")
    String correctedContent,

    String nativeSpeakerTips,

    @Min(1) @Max(5)
    Integer rating
) {}
