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
    Instant reviewedAt,
    String reviewStatus,
    List<QuestionFeedbackDTO> questionResults
) {
    public ExerciseSubmissionResponseDTO(
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
            Instant reviewedAt) {
        this(
            id,
            exerciseId,
            exerciseTitle,
            languageName,
            studentId,
            studentName,
            answers,
            correctCount,
            questionCount,
            xpEarned,
            feedback,
            submittedAt,
            reviewedAt,
            "AUTO_GRADED",
            List.of()
        );
    }

    public record QuestionFeedbackDTO(
        int questionIndex,
        String style,
        String status,
        String message
    ) {
    }
}
