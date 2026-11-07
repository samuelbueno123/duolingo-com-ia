package com.duolinfo.ia.proj.dto.srs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpacedReviewItemRequestDTO(
    @NotBlank(message = "O idioma é obrigatório.")
    String languageName,

    @NotBlank(message = "O termo ou expressão é obrigatório.")
    @Size(max = 250)
    String term,

    @NotBlank(message = "A tradução ou significado é obrigatório.")
    @Size(max = 250)
    String translation,

    String exampleSentence,
    String exampleTranslation,
    String proficiencyLevel,
    String tags
) {
}
