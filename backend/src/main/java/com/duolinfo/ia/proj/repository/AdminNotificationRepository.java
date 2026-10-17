package com.duolinfo.ia.proj.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.duolinfo.ia.proj.entity.AdminNotification;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {

    List<AdminNotification> findAllByOrderByCreatedAtDesc();

    List<AdminNotification> findByReferenceTypeAndReferenceId(
        String referenceType,
        Long referenceId);

    List<AdminNotification> findByInstitutionIdOrderByCreatedAtDesc(Long institutionId);

    long countByReadFalse();

    long countByInstitutionIdAndReadFalse(Long institutionId);

}
