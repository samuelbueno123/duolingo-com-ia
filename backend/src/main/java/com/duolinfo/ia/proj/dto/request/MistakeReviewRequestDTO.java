package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.NotNull;

public record MistakeReviewRequestDTO(
    @NotNull(message = "O resultado do acerto é obrigatório.")
    Boolean isCorrect
) {}
