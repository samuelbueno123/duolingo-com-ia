package com.duolinfo.ia.proj.dto.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExerciseSubmissionResponseDTO(
    Long id,
    Long exerciseId,
    String exerciseTitle,
    String languageName,
    Long studentId,
    String studentName,
    String studentProfilePicture,
    List<String> answers,
    int correctCount,
    int questionCount,
    int xpEarned,
    String feedback,
    Instant submittedAt,
    Instant reviewedAt,
    String reviewStatus,
    @JsonProperty("questionResults")
    @JsonAlias({"questionFeedback", "questions"})
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
            null,
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
            Instant reviewedAt,
            String reviewStatus,
            List<QuestionFeedbackDTO> questionResults) {
        this(
            id,
            exerciseId,
            exerciseTitle,
            languageName,
            studentId,
            studentName,
            null,
            answers,
            correctCount,
            questionCount,
            xpEarned,
            feedback,
            submittedAt,
            reviewedAt,
            reviewStatus,
            questionResults
        );
    }

    @JsonProperty("questionFeedback")
    public List<QuestionFeedbackDTO> questionFeedback() {
        return questionResults;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuestionFeedbackDTO(
        int questionIndex,
        String style,
        String status,
        String message,
        String question,
        List<String> options,
        String expectedAnswer,
        String studentAnswer,
        Boolean isCorrect
    ) {
        public QuestionFeedbackDTO(
            int questionIndex,
            String style,
            String status,
            String message
        ) {
            this(
                questionIndex,
                style,
                status,
                message,
                null,
                List.of(),
                null,
                null,
                "CORRECT".equalsIgnoreCase(status)
            );
        }
    }
}
