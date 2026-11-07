package com.duolinfo.ia.proj.dto.srs;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SpacedReviewAnswerDTO(
    @NotNull(message = "A nota de avaliação é obrigatória.")
    @Min(value = 1, message = "A nota deve ser entre 1 (Errei) e 4 (Fácil).")
    @Max(value = 4, message = "A nota deve ser entre 1 (Errei) e 4 (Fácil).")
    Integer rating
) {
}
