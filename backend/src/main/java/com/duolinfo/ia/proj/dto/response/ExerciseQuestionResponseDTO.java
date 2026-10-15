package com.duolinfo.ia.proj.dto.response;

import java.util.List;

public record ExerciseQuestionResponseDTO(
    String style,
    String question,
    List<String> options,
    String correctAnswer,
    String expectedAnswer
) {
}
