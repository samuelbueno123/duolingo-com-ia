package com.duolinfo.ia.proj.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record CloneExerciseRequestDTO(
    @NotNull(message = "A turma de destino é obrigatória.")
    Long targetSchoolClassId,

    LocalDate newDueDate,

    String newTitle
) {}
