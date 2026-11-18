package com.duolinfo.ia.proj.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record ExerciseQuestionRequestDTO(
    String style,
    @NotBlank String question,
    List<String> options,
    String correctAnswer,
    String expectedAnswer,
    String mascotHelpMessage,
    String audioText
) {
    public ExerciseQuestionRequestDTO(
        String style,
        String question,
        List<String> options,
        String correctAnswer,
        String expectedAnswer
    ) {
        this(style, question, options, correctAnswer, expectedAnswer, null, null);
    }

    public ExerciseQuestionRequestDTO(
        String style,
        String question,
        List<String> options,
        String correctAnswer,
        String expectedAnswer,
        String mascotHelpMessage
    ) {
        this(style, question, options, correctAnswer, expectedAnswer, mascotHelpMessage, null);
    }
}
