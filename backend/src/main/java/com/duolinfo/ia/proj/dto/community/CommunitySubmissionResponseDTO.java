package com.duolinfo.ia.proj.dto.community;

import java.time.LocalDateTime;
import java.util.List;

import com.duolinfo.ia.proj.entity.CommunitySubmission;

public record CommunitySubmissionResponseDTO(
    Long id,
    Long authorStudentId,
    String authorName,
    String language,
    String promptTitle,
    String content,
    String audioUrl,
    String targetLevel,
    Boolean isPublic,
    String status,
    Integer reviewCount,
    LocalDateTime createdAt,
    List<CommunityReviewResponseDTO> reviews
) {
    public static CommunitySubmissionResponseDTO fromEntity(CommunitySubmission sub, List<CommunityReviewResponseDTO> reviews) {
        return new CommunitySubmissionResponseDTO(
            sub.getId(),
            sub.getAuthorStudentId(),
            sub.getAuthorName(),
            sub.getLanguage(),
            sub.getPromptTitle(),
            sub.getContent(),
            sub.getAudioUrl(),
            sub.getTargetLevel(),
            sub.getIsPublic(),
            sub.getStatus(),
            sub.getReviewCount(),
            sub.getCreatedAt(),
            reviews
        );
    }
}
