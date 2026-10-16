package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.duolinfo.ia.proj.entity.InstitutionRequest;
import com.duolinfo.ia.proj.entity.InstitutionRequest.Status;

public interface InstitutionRequestRepository extends JpaRepository<InstitutionRequest, Long> {

    List<InstitutionRequest> findAllByOrderByCreatedAtDesc();

    List<InstitutionRequest> findByStatusOrderByCreatedAtDesc(Status status);

    Optional<InstitutionRequest> findByIdAndStatus(Long id, Status status);
}
