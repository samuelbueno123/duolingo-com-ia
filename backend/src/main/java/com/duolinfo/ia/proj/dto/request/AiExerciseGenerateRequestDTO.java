package com.duolinfo.ia.proj.dto.request;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiExerciseGenerateRequestDTO(
    @NotBlank(message = "Informe o idioma do exercício.")
    @Size(max = 60)
    String languageName,

    @NotBlank(message = "Informe o nível de dificuldade.")
    @Size(max = 40)
    String proficiencyLevel,

    @Size(max = 60)
    String category,

    @NotBlank(message = "Informe o tema ou subtópico para gerar as questões.")
    @Size(max = 160)
    String theme,

    @Min(value = 1, message = "Gere pelo menos 1 questão.")
    @Max(value = 10, message = "Gere no máximo 10 questões por vez.")
    int questionCount,

    List<String> questionStyles,

    @Size(max = 500, message = "As instruções adicionais podem ter no máximo 500 caracteres.")
    String additionalInstructions
) {
}
