package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.CommunityReport;

@Repository
public interface CommunityReportRepository extends JpaRepository<CommunityReport, Long> {
    List<CommunityReport> findByReportedReviewId(Long reportedReviewId);
}
