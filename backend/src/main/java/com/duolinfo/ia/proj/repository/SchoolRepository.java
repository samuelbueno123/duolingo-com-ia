package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.School;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {

    Optional<School> findByInepCode(String inepCode);

    boolean existsByInepCode(String inepCode);

    List<School> findByNameContainingIgnoreCase(String name);

    List<School> findByInstitutionId(Long institutionId);

    List<School> findByInstitutionIdAndNameContainingIgnoreCase(
        Long institutionId,
        String name
    );

    List<School> findByInstitutionIdAndInepCode(
        Long institutionId,
        String inepCode
    );
}