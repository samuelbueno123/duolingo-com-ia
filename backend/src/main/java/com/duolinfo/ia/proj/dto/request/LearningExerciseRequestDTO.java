package com.duolinfo.ia.proj.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LearningExerciseRequestDTO(
    @NotBlank String title,
    String category,
    String theme,
    @NotBlank String languageName,
    @NotBlank String proficiencyLevel,
    String description,
    String contentBody,
    String mediaUrl,
    LocalDate dueDate,
    @NotNull Long schoolClassId,
    List<@Valid ExerciseQuestionRequestDTO> questions,
    String mascotHelpMessage
) {
    public LearningExerciseRequestDTO(
            String title,
            String category,
            String theme,
            String languageName,
            String proficiencyLevel,
            String description,
            String contentBody,
            String mediaUrl,
            LocalDate dueDate,
            Long schoolClassId,
            List<ExerciseQuestionRequestDTO> questions) {
        this(
            title,
            category,
            theme,
            languageName,
            proficiencyLevel,
            description,
            contentBody,
            mediaUrl,
            dueDate,
            schoolClassId,
            questions,
            null
        );
    }
}
