package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.NotNull;

public record StudentJoinClassRequestDTO(
    @NotNull(message = "O ID da turma é obrigatório.")
    Long schoolClassId,
    String notes
) {}
