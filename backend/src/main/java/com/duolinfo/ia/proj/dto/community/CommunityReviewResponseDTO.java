package com.duolinfo.ia.proj.dto.community;

import java.time.LocalDateTime;

import com.duolinfo.ia.proj.entity.CommunityReview;

public record CommunityReviewResponseDTO(
    Long id,
    Long submissionId,
    Long reviewerStudentId,
    String reviewerName,
    String reviewerFluencyLevel,
    String overallFeedback,
    String correctedContent,
    String nativeSpeakerTips,
    Integer rating,
    LocalDateTime createdAt
) {
    public static CommunityReviewResponseDTO fromEntity(CommunityReview r) {
        return new CommunityReviewResponseDTO(
            r.getId(),
            r.getSubmissionId(),
            r.getReviewerStudentId(),
            r.getReviewerName(),
            r.getReviewerFluencyLevel(),
            r.getOverallFeedback(),
            r.getCorrectedContent(),
            r.getNativeSpeakerTips(),
            r.getRating(),
            r.getCreatedAt()
        );
    }
}
