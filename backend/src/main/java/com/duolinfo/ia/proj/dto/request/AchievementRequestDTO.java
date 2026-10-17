package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AchievementRequestDTO(
    @NotBlank(message = "O código é obrigatório.")
    String code,

    @NotBlank(message = "O título é obrigatório.")
    String title,

    @NotBlank(message = "A descrição é obrigatória.")
    String description,

    @NotBlank(message = "A categoria é obrigatória.")
    String category,

    String iconName,

    Integer displayOrder
) {
}
