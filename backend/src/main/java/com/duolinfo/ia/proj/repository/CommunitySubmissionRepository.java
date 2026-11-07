package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.CommunitySubmission;

@Repository
public interface CommunitySubmissionRepository extends JpaRepository<CommunitySubmission, Long> {
    List<CommunitySubmission> findByAuthorStudentIdOrderByCreatedAtDesc(Long authorStudentId);
    List<CommunitySubmission> findByLanguageIgnoreCaseAndIsPublicTrueOrderByCreatedAtDesc(String language);
    List<CommunitySubmission> findByIsPublicTrueOrderByCreatedAtDesc();
}
