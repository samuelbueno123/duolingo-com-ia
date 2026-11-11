package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DailyGoalRequestDTO(
    @NotNull(message = "A meta diária de XP é obrigatória.")
    @Min(value = 10, message = "A meta diária deve ser de pelo menos 10 XP.")
    @Max(value = 500, message = "A meta diária máxima é de 500 XP.")
    Integer dailyXpGoal
) {}
