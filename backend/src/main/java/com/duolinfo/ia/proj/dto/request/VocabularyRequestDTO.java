package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VocabularyRequestDTO(
    @NotBlank(message = "O idioma é obrigatório.")
    String language,

    @NotBlank(message = "O termo é obrigatório.")
    @Size(max = 120, message = "O termo deve ter no máximo 120 caracteres.")
    String term,

    @NotBlank(message = "A tradução é obrigatória.")
    @Size(max = 200, message = "A tradução deve ter no máximo 200 caracteres.")
    String translation,

    String partOfSpeech,

    String exampleSentence,

    String masteryLevel // LEARNING, REVIEWED, MASTERED
) {}
