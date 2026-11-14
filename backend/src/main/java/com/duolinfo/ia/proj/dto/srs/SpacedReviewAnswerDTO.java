package com.duolinfo.ia.proj.dto.srs;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SpacedReviewAnswerDTO(
    @NotNull(message = "A nota de avaliação é obrigatória.")
    @Min(value = 0, message = "A nota deve ser entre 0 e 5.")
    @Max(value = 5, message = "A nota deve ser entre 0 e 5.")
    Integer rating
) {
}
