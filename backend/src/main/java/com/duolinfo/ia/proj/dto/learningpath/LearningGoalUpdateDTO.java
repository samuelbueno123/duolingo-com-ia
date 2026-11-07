package com.duolinfo.ia.proj.dto.learningpath;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LearningGoalUpdateDTO(
    @NotBlank(message = "O idioma é obrigatório.")
    String language,

    @NotNull(message = "A meta diária de minutos é obrigatória.")
    @Min(value = 5, message = "A meta mínima diária é 5 minutos.")
    @Max(value = 120, message = "A meta máxima diária é 120 minutos.")
    Integer dailyGoalMinutes,

    String targetCefrLevel
) {}
