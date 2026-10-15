package com.duolinfo.ia.proj.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record ExerciseQuestionRequestDTO(
    String style,
    @NotBlank String question,
    List<String> options,
    String correctAnswer,
    String expectedAnswer
) {
}
