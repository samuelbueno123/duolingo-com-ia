package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    long countBySeverity(String severity);

    long countByCategory(String category);

    long countByResult(String result);

    List<AuditLog> findTop10ByOrderByTimestampDesc();

    List<AuditLog> findTop50ByOrderByTimestampDesc();
}
