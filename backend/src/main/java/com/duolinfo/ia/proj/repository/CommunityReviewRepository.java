package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.CommunityReview;

@Repository
public interface CommunityReviewRepository extends JpaRepository<CommunityReview, Long> {
    List<CommunityReview> findBySubmissionIdOrderByCreatedAtDesc(Long submissionId);
    List<CommunityReview> findByReviewerStudentIdOrderByCreatedAtDesc(Long reviewerStudentId);
    boolean existsBySubmissionIdAndReviewerStudentId(Long submissionId, Long reviewerStudentId);
}
