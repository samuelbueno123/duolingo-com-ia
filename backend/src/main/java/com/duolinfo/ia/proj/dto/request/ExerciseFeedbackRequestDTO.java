package com.duolinfo.ia.proj.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExerciseFeedbackRequestDTO(
    String feedback,
    Integer correctCount,
    List<QuestionReviewItemDTO> questionReviews
) {
    public ExerciseFeedbackRequestDTO(String feedback) {
        this(feedback, null, null);
    }

    public ExerciseFeedbackRequestDTO(String feedback, Integer correctCount) {
        this(feedback, correctCount, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuestionReviewItemDTO(
        Integer questionIndex,
        Boolean isCorrect,
        String status,
        String feedback
    ) {
    }
}
