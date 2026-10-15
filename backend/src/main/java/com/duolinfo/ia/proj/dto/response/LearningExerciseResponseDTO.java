package com.duolinfo.ia.proj.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LearningExerciseResponseDTO(
    Long id,
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
    String schoolClassName,
    Long teacherId,
    String teacherName,
    Instant createdAt,
    List<ExerciseQuestionResponseDTO> questions
) {
}
