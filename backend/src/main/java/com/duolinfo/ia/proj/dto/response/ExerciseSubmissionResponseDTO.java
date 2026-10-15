package com.duolinfo.ia.proj.dto.response;

import java.time.Instant;
import java.util.List;

public record ExerciseSubmissionResponseDTO(
    Long id,
    Long exerciseId,
    String exerciseTitle,
    String languageName,
    Long studentId,
    String studentName,
    List<String> answers,
    int correctCount,
    int questionCount,
    int xpEarned,
    String feedback,
    Instant submittedAt,
    Instant reviewedAt
) {
}
