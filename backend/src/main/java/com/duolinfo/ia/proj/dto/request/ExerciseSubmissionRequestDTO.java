package com.duolinfo.ia.proj.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record ExerciseSubmissionRequestDTO(
    @NotNull List<@NotNull String> answers
) {
}
