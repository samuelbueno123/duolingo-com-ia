package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ExerciseFeedbackRequestDTO(@NotBlank String feedback) {
}
